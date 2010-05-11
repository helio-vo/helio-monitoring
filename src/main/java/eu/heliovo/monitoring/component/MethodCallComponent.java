package eu.heliovo.monitoring.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import com.eviware.soapui.impl.wsdl.WsdlSubmitContext;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;
import com.eviware.soapui.model.iface.Request.SubmitException;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.support.SoapUIException;

import eu.heliovo.monitoring.logging.DummyLogFileWriter;
import eu.heliovo.monitoring.logging.LogFileWriter;
import eu.heliovo.monitoring.logging.LogFileWriterImpl;
import eu.heliovo.monitoring.logging.LoggingHelper;
import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.State;
import eu.heliovo.monitoring.util.WsdlValidationUtils;

@Component
public final class MethodCallComponent extends AbstractComponent {

	private final Logger logger = Logger.getLogger(this.getClass());
	private final String logFilesDirectory;
	private final String logFilesUrl;
	private final LogFileWriter dummyLogFileWriter = new DummyLogFileWriter();

	// TODO enable configuration of these parameters from resource file helio-monitoring.properties
	private static final int IMPORT_WSDL_TIMEOUT = 4000; // in milliseconds
	private static final int WAIT_FOR_RESPONSE_TIMEOUT = 10000; // in milliseconds
	private static final boolean GENERATE_OPTINAL_PARAMS = false;
	private static final String LOG_FILE_SUFFIX = "_method-call_";

	@Autowired
	public MethodCallComponent(@Value("${methodCall.log.filePath}") final String logFilesDirectory,
			@Value("${monitoringService.logUrl}") final String logFilesUrl) {

		super(" -method call-");

		this.logFilesDirectory = logFilesDirectory;
		this.logFilesUrl = logFilesUrl;
	}

	// TODO cache WSDL documents to avoid the import on every run
	// TODO use ExecuterService for concurrency
	// see http://www.soapui.org/architecture/integration.html
	// see http://www.soapui.org/userguide/functional/response-assertions.html, download source of soapUI &
	// search for files containing "assertion"
	@Override
	public void refreshCache() {

		LoggingHelper.deleteLogFilesOlderThanOneDay(logFilesDirectory, logger);

		final List<ServiceStatus> newCache = new ArrayList<ServiceStatus>();

		for (final Service service : super.getServices()) {

			final String serviceName = service.getName() + super.getServiceNameSuffix();
			LogFileWriter logFileWriter = dummyLogFileWriter;

			try {
				logFileWriter = new LogFileWriterImpl(logFilesDirectory, service.getName() + LOG_FILE_SUFFIX);

				final WsdlInterface wsdlInterface = importWsdl(logFileWriter, service.getUrl().toString());
				final WsdlRequest request = createRequest(wsdlInterface, logFileWriter);
				final WsdlResponse response = submitRequest(request, logFileWriter);
				final ServiceStatus serviceStatus = processResponse(response, serviceName, service, logFileWriter);

				newCache.add(serviceStatus);
				wsdlInterface.getProject().release();

			} catch (final RuntimeException e) {
				handleException(e, logFileWriter, serviceName, service, newCache);
			} catch (final SubmitException e) {
				handleException(e, logFileWriter, serviceName, service, newCache);
			} catch (final XmlException e) {
				handleException(e, logFileWriter, serviceName, service, newCache);
			} catch (final IOException e) {
				handleException(e, logFileWriter, serviceName, service, newCache);
			} catch (final SoapUIException e) {
				handleException(e, logFileWriter, serviceName, service, newCache);
			} catch (final InterruptedException e) {
				handleException(e, logFileWriter, serviceName, service, newCache);
			}

			logFileWriter.close();
		}

		super.setCache(newCache);
	}

	private WsdlInterface importWsdl(final LogFileWriter logFileWriter, final String wsdlUrl) throws IOException,
			SoapUIException, XmlException, InterruptedException {

		final WsdlProject project = new WsdlProject();

		logger.debug("Importing WSDL file " + wsdlUrl);
		logFileWriter.writeToLogFile("Importing WSDL file " + wsdlUrl);

		final ImportWsdlRunnable importWsdlRunnable = new ImportWsdlRunnable(project, wsdlUrl);
		final Thread importWsdlThread = new Thread(importWsdlRunnable);
		importWsdlThread.start();
		importWsdlThread.join(IMPORT_WSDL_TIMEOUT);

		if (importWsdlRunnable.soapUIException != null) {
			throw importWsdlRunnable.soapUIException;
		}

		if (importWsdlRunnable.wsdlInterface == null) { // timeout by join operation
			project.release();
			throw new IllegalStateException("Importing the WSDL file timed out, timeout: " + IMPORT_WSDL_TIMEOUT
					+ " ms");
		}

		logger.debug("Importing finished");
		logFileWriter.writeToLogFile("Importing finished");

		return importWsdlRunnable.wsdlInterface;
	}

	private WsdlRequest createRequest(final WsdlInterface wsdlInterface, final LogFileWriter logFileWriter) {

		// TODO test best strategy + error handling (choose first, a random or all method(s) of wsdl file)
		final WsdlOperation operation = wsdlInterface.getOperationAt(0);
		// wsdlInterface.getOperationCount();
		// wsdlInterface.getOperationList();

		final WsdlRequest request = operation.addNewRequest(operation.getName());

		// generate the request content from the schema
		request.setRequestContent(operation.createRequest(GENERATE_OPTINAL_PARAMS));

		logger.debug("=== Request Content ===");
		logger.debug(request.getRequestContent());
		logFileWriter.writeToLogFile("=== Request Content ===");
		logFileWriter.writeToLogFile(request.getRequestContent());

		// validate request
		final AssertionError[] requestAssertionErrors = WsdlValidationUtils.validateRequest(request);
		LoggingHelper.logRequestValidation(requestAssertionErrors, logFileWriter, logger);

		return request;
	}

	private WsdlResponse submitRequest(final WsdlRequest request, final LogFileWriter logFileWriter)
			throws InterruptedException, SubmitException {

		logger.debug("Sending request");
		logFileWriter.writeToLogFile("Sending request");

		final SubmitRequestRunnable submitRequestRunnable = new SubmitRequestRunnable(request);
		final Thread submitRequestThread = new Thread(submitRequestRunnable);

		logger.debug("Waiting for the response");
		logFileWriter.writeToLogFile("Waiting for the response");

		submitRequestThread.start();
		submitRequestThread.join(WAIT_FOR_RESPONSE_TIMEOUT);

		if (submitRequestRunnable.submitException != null) {
			throw submitRequestRunnable.submitException;
		}

		if (submitRequestRunnable.response == null) { // timeout by join operation
			throw new IllegalStateException("Waiting for the response timed out, timeout: " + WAIT_FOR_RESPONSE_TIMEOUT
					+ " ms");
		}

		return submitRequestRunnable.response;
	}

	private ServiceStatus processResponse(final WsdlResponse response, final String serviceName, final Service service,
			final LogFileWriter logFileWriter) throws XmlException {

		final long responseTime = response.getTimeTaken();

		final String responseTimeText = ", response time = " + responseTime + " ms";
		logger.debug("Response received" + responseTimeText);
		logFileWriter.writeToLogFile("Response received" + responseTimeText);

		logger.debug("=== Response Content ===");
		logFileWriter.writeToLogFile("=== Response Content ===");

		final String content = response.getContentAsString();
		logger.debug(content);
		logFileWriter.writeToLogFile(content);

		// build status information

		// build message
		final StringBuffer stringBuffer = new StringBuffer("Service is working");
		State state = State.OK;

		// response validation
		final AssertionError[] responseAssertionErrors = WsdlValidationUtils.validateResponse(response);
		LoggingHelper.logResponseValidation(responseAssertionErrors, logFileWriter, logger);
		if (responseAssertionErrors != null && responseAssertionErrors.length > 0) {
			stringBuffer.append(", but the repsonse is not valid");
			state = State.CRITICAL;
		}

		// SOAP fault test
		final WsdlOperation operation = response.getRequest().getOperation();
		if (WsdlValidationUtils.isSoapFault(content, operation)) {
			stringBuffer.append(", but the response is a SOAP fault");
			logger.debug("The response is a SOAP fault!");
			logFileWriter.writeToLogFile("The response is a SOAP fault!");
			state = State.WARNING;
		}

		stringBuffer.append(responseTimeText);
		stringBuffer.append(LoggingHelper.getLogFileText(logFileWriter, logFilesUrl));
		final String message = stringBuffer.toString();

		return new ServiceStatus(serviceName, service.getUrl(), state, responseTime, message);
	}

	private void handleException(final Exception e, final LogFileWriter logFileWriter, final String serviceName,
			final Service service, final List<ServiceStatus> newCache) {

		final String message = "An error occured: " + e.getMessage()
				+ LoggingHelper.getLogFileText(logFileWriter, logFilesUrl);
		final ServiceStatus status = new ServiceStatus(serviceName, service.getUrl(), State.CRITICAL, 0, message);
		newCache.add(status);

		logFileWriter.writeToLogFile("An error occured: " + e.getMessage());
		logFileWriter.writeStacktracetoLogFile(e);
	}

	private final static class ImportWsdlRunnable implements Runnable {

		private final WsdlProject wsdlProject;
		private final String url;
		private WsdlInterface wsdlInterface;
		private SoapUIException soapUIException;

		public ImportWsdlRunnable(final WsdlProject wsdlProject, final String url) {
			this.wsdlProject = wsdlProject;
			this.url = url;
		}

		@Override
		public void run() {
			try {
				wsdlInterface = WsdlInterfaceFactory.importWsdl(wsdlProject, url, true)[0];
			} catch (final SoapUIException e) {
				soapUIException = e;
			}
		}

	}

	private final static class SubmitRequestRunnable implements Runnable {

		private final WsdlRequest request;
		private WsdlResponse response;
		private SubmitException submitException;

		private final static boolean ASYNC = false;

		public SubmitRequestRunnable(final WsdlRequest request) {
			this.request = request;
		}

		@Override
		public void run() {
			try {
				final WsdlSubmitContext wsdlSubmitContext = new WsdlSubmitContext(request.getModelItem());
				response = (WsdlResponse) request.submit(wsdlSubmitContext, ASYNC).getResponse();
			} catch (final SubmitException e) {
				submitException = e;
			}
		}
	}
}
