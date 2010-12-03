package eu.heliovo.monitoring.component;

import static eu.heliovo.monitoring.model.ServiceFactory.newServiceStatusDetails;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eviware.soapui.impl.WsdlInterfaceFactory;
import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.impl.wsdl.WsdlSubmit;
import com.eviware.soapui.impl.wsdl.WsdlSubmitContext;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;
import com.eviware.soapui.model.iface.Request.SubmitException;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.support.SoapUIException;

import eu.heliovo.monitoring.logging.LogFileWriter;
import eu.heliovo.monitoring.logging.LoggingHelper;
import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.ServiceStatusDetails;
import eu.heliovo.monitoring.util.WsdlValidationUtils;

/**
 * Contains common code of the Components.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class ComponentHelper {

	private final Logger logger = Logger.getLogger(this.getClass());

	private static final int IMPORT_WSDL_TIMEOUT = 10;
	private static final boolean GENERATE_OPTINAL_PARAMS = false;
	private static final boolean SUBMIT_ASYNC = false;
	private static final int RESPONSE_TIMEOUT = 10;

	private static final int FIRST_WSDL_INTERFACE = 0;

	private final ExecutorService executor;
	private final String logFilesUrl;

	@Autowired
	public ComponentHelper(ExecutorService executor, @Value("${monitoringService.logUrl}") String logFilesUrl) {
		this.executor = executor;
		this.logFilesUrl = logFilesUrl;
	}

	protected WsdlInterface importWsdl(LogFileWriter logFileWriter, final String wsdlUrl) throws XmlException,
			IOException, SoapUIException, ExecutionException, InterruptedException {

		final WsdlProject project = new WsdlProject();

		logger.debug("Importing WSDL file " + wsdlUrl);
		logFileWriter.writeToLogFile("Importing WSDL file " + wsdlUrl);

		Future<WsdlInterface> future = executor.submit(new Callable<WsdlInterface>() {
			public WsdlInterface call() throws SoapUIException {
				WsdlInterface[] wsdlInterfaces = WsdlInterfaceFactory.importWsdl(project, wsdlUrl, true);
				return wsdlInterfaces[FIRST_WSDL_INTERFACE];
			}
		});

		try {
			// TODO automatically determine timeout
			WsdlInterface wsdlInterface = future.get(IMPORT_WSDL_TIMEOUT, TimeUnit.SECONDS);

			logger.debug("Importing finished");
			logFileWriter.writeToLogFile("Importing finished");

			return wsdlInterface;

		} catch (TimeoutException e) {
			project.release();
			throw new IllegalStateException("Importing WSDL file timed out (timeout: " + IMPORT_WSDL_TIMEOUT + " s)");
		}
	}

	protected WsdlRequest createRequest(WsdlInterface wsdlInterface, LogFileWriter logFileWriter,
			WsdlOperation operation) {

		String requestContent = operation.createRequest(GENERATE_OPTINAL_PARAMS);
		return this.createRequest(wsdlInterface, logFileWriter, operation, requestContent);
	}

	protected WsdlRequest createRequest(WsdlInterface wsdlInterface, LogFileWriter logFileWriter,
			WsdlOperation operation, String requestContent) {

		WsdlRequest request = operation.addNewRequest(operation.getName());

		// generate the request content from the schema
		request.setRequestContent(requestContent);

		logger.debug("=== Request Content for Operation \"" + operation.getName() + "\" ===");
		logger.debug(request.getRequestContent());
		logFileWriter.writeToLogFile("=== Request Content for Operation \"" + operation.getName() + "\" ===");
		logFileWriter.writeToLogFile(request.getRequestContent());

		// validate request
		AssertionError[] requestAssertionErrors = WsdlValidationUtils.validateRequest(request);
		LoggingHelper.logRequestValidation(requestAssertionErrors, logFileWriter);

		return request;
	}

	protected WsdlResponse submitRequest(final WsdlRequest request, LogFileWriter logFileWriter)
			throws ExecutionException, InterruptedException, SubmitException {

		logger.debug("Sending request");
		logFileWriter.writeToLogFile("Sending request");

		Future<WsdlResponse> future = executor.submit(new Callable<WsdlResponse>() {
			public WsdlResponse call() throws SubmitException {

				WsdlSubmitContext wsdlSubmitContext = new WsdlSubmitContext(request.getModelItem());
				WsdlSubmit<WsdlRequest> submit = request.submit(wsdlSubmitContext, SUBMIT_ASYNC);

				return (WsdlResponse) submit.getResponse();
			}
		});

		logger.debug("Waiting for the response");
		logFileWriter.writeToLogFile("Waiting for the response");

		try {
			// TODO automatically determine timeout
			return future.get(RESPONSE_TIMEOUT, TimeUnit.SECONDS);

		} catch (TimeoutException e) {
			throw new IllegalStateException("Waiting for response timed out (timeout: " + RESPONSE_TIMEOUT + " s)");
		}
	}

	protected void processResponse(WsdlResponse response, String serviceName, Service service,
			LogFileWriter logFileWriter) throws XmlException {

		long responseTime = response.getTimeTaken();
		logger.debug("Response received, response time = " + responseTime + " ms");
		logFileWriter.writeToLogFile("Response received, response time = " + responseTime + " ms");

		WsdlOperation operation = response.getRequest().getOperation();
		logger.debug("=== Response Content for Operation \"" + operation.getName() + "\" ===");
		logFileWriter.writeToLogFile("=== Response Content for Operation \"" + operation.getName() + "\" ===");

		String content = response.getContentAsString();
		logger.debug(content);
		logFileWriter.writeToLogFile(content);
	}

	protected void handleException(Exception e, LogFileWriter logFileWriter, String serviceName, Service service,
			List<ServiceStatusDetails> newCache) {

		String message = "An error occured: " + e.getMessage()
				+ LoggingHelper.getLogFileText(logFileWriter, logFilesUrl);

		newCache.add(newServiceStatusDetails(serviceName, service.getUrl(), ServiceStatus.CRITICAL, 0, message));

		logFileWriter.writeToLogFile("An error occured: " + e.getMessage());
		logFileWriter.writeStacktracetoLogFile(e);
	}
}