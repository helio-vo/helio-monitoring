package eu.heliovo.monitoring.component;

import static eu.heliovo.monitoring.model.ModelFactory.newServiceStatusDetails;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;

import com.eviware.soapui.impl.wsdl.*;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;
import com.eviware.soapui.model.iface.Request.SubmitException;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.support.SoapUIException;

import eu.heliovo.monitoring.command.ImportWsdlCommand;
import eu.heliovo.monitoring.logging.*;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.util.WsdlValidationUtils;

/**
 * Contains common code of the Components.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class ComponentHelper {

	private static final boolean GENERATE_OPTINAL_PARAMS = false;
	private static final boolean SUBMIT_ASYNC = false;
	private static final int RESPONSE_TIMEOUT = 10;

	private final ExecutorService executor;
	private final String logFilesUrl;

	@Autowired
	public ComponentHelper(ExecutorService executor, @Value("${monitoringService.logUrl}") String logFilesUrl) {
		this.executor = executor;
		this.logFilesUrl = logFilesUrl;
	}

	protected WsdlInterface importWsdl(LogFileWriter logFileWriter, final String wsdlUrl) throws XmlException,
			IOException, SoapUIException, ExecutionException, InterruptedException {

		return new ImportWsdlCommand(logFileWriter, wsdlUrl, executor).execute();
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

		logFileWriter.write("=== Request Content for Operation \"" + operation.getName() + "\" ===");
		logFileWriter.write(request.getRequestContent());

		// validate request
		AssertionError[] requestAssertionErrors = WsdlValidationUtils.validateRequest(request);
		LoggingHelper.logRequestValidation(requestAssertionErrors, logFileWriter);

		return request;
	}

	protected WsdlResponse submitRequest(final WsdlRequest request, LogFileWriter logFileWriter)
			throws ExecutionException, InterruptedException, SubmitException {

		logFileWriter.write("Sending request");

		Future<WsdlResponse> future = executor.submit(new Callable<WsdlResponse>() {
			@Override
			public WsdlResponse call() throws SubmitException {

				WsdlSubmitContext wsdlSubmitContext = new WsdlSubmitContext(request.getModelItem());
				WsdlSubmit<WsdlRequest> submit = request.submit(wsdlSubmitContext, SUBMIT_ASYNC);

				return (WsdlResponse) submit.getResponse();
			}
		});

		logFileWriter.write("Waiting for the response");

		try {
			// TODO automatically determine timeout
			return future.get(RESPONSE_TIMEOUT, TimeUnit.SECONDS);

		} catch (TimeoutException e) {
			throw new IllegalStateException("Waiting for response timed out (timeout: " + RESPONSE_TIMEOUT + " s)");
		} finally {
			future.cancel(true);
		}
	}

	protected void processResponse(WsdlResponse response, String serviceName, Service service,
			LogFileWriter logFileWriter) throws XmlException {

		long responseTime = response.getTimeTaken();
		logFileWriter.write("Response received, response time = " + responseTime + " ms");

		WsdlOperation operation = response.getRequest().getOperation();
		logFileWriter.write("=== Response Content for Operation \"" + operation.getName() + "\" ===");

		String content = response.getContentAsString();
		logFileWriter.write(content);
	}

	protected void handleException(Exception exception, LogFileWriter logFileWriter, String serviceName,
			Service service,
			List<ServiceStatusDetails> newCache) {

		String message = "An error occured: " + exception.getMessage()
				+ LoggingHelper.getLogFileText(logFileWriter, logFilesUrl);

		newCache.add(newServiceStatusDetails(serviceName, service.getUrl(), ServiceStatus.CRITICAL, 0, message));

		logFileWriter.write(exception);
	}
}