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
public class MethodCallComponent extends AbstractComponent {

	protected Logger logger = Logger.getLogger(this.getClass());
	private final String logFilesDirectory;
	private final String logFilesUrl;
	private final LogFileWriter dummyLogFileWriter = new DummyLogFileWriter();

	// TODO enable configuration of these parameters from resource file helio-monitoring.properties
	private final static int IMPORT_WSDL_TIMEOUT = 4000; // in milliseconds
	private final static int WAIT_FOR_RESPONSE_TIMEOUT = 10000; // in milliseconds
	private final static boolean GENERATE_OPTINAL_PARAMS = false;
	private final static String LOG_FILE_SUFFIX = "_method-call_";

	@Autowired
	public MethodCallComponent(@Value("${methodCall.log.filePath}") final String logFilesDirectory,
			@Value("${monitoringService.logUrl}") final String logFilesUrl) {

		super(" -method call-");

		this.logFilesDirectory = logFilesDirectory;
		this.logFilesUrl = logFilesUrl;
	}

	// TODO refactor method
	// TODO cache WSDL documents to avoid the import on every run
	// TODO use ExecuterService for concurrency
	// TODO create a class for saving the state of a service and generate messages & logs accordingly
	@Override
	public void refreshCache() {

		LoggingHelper.deleteLogFilesOlderThanOneDay(logFilesDirectory, logger);

		final List<ServiceStatus> newCache = new ArrayList<ServiceStatus>();

		for (final Service service : super.services) {

			final String serviceName = service.getName() + SERVICE_NAME_SUFFIX;
			LogFileWriter logFileWriter = null;

			try {

				try {
					logFileWriter = new LogFileWriterImpl(logFilesDirectory, service.getName() + LOG_FILE_SUFFIX);
				} catch (final RuntimeException e) {
					logFileWriter = dummyLogFileWriter;
					throw e;
				}

				// see http://www.soapui.org/architecture/integration.html
				// see http://www.soapui.org/userguide/functional/response-assertions.html, download source of soapUI &
				// search for files containing "assertion"

				final WsdlProject project = new WsdlProject();

				logger.debug("Importing WSDL file " + service.getUrl());
				logFileWriter.writeToLogFile("Importing WSDL file " + service.getUrl());

				final ImportWsdlRunnable importWsdlRunnable = new ImportWsdlRunnable(project, service.getUrl()
						.toString());
				final Thread importWsdlThread = new Thread(importWsdlRunnable);
				importWsdlThread.start();
				importWsdlThread.join(IMPORT_WSDL_TIMEOUT);

				if (importWsdlRunnable.soapUIException != null) {
					throw importWsdlRunnable.soapUIException;
				} else if (importWsdlRunnable.wsdlInterface == null) { // timeout by join operation

					final ServiceStatus status = new ServiceStatus(serviceName, service.getUrl(), State.CRITICAL, 0);

					final String message = "Importing the WSDL file timed out, timeout: " + IMPORT_WSDL_TIMEOUT + " ms";
					status.setMessage(message + LoggingHelper.getLogFileText(logFileWriter, logFilesUrl));
					logger.debug(message);
					logFileWriter.writeToLogFile(message);

					newCache.add(status);

				} else {

					logger.debug("Importing finished");
					logFileWriter.writeToLogFile("Importing finished");

					// TODO test best strategy + error handling (choose first, a random or all method(s) of wsdl file)
					final WsdlOperation operation = importWsdlRunnable.wsdlInterface.getOperationAt(0);
					// wsdlInterface.getOperationCount();
					// wsdlInterface.getOperationList();

					final WsdlRequest request = operation.addNewRequest(operation.getName());

					// to set content manually: request.setRequestContent(request);

					// maybe for method parameters:
					// request.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);

					// generate the request content from the schema
					request.setRequestContent(operation.createRequest(GENERATE_OPTINAL_PARAMS));

					logger.debug("=== Request Content ===");
					logger.debug(request.getRequestContent());
					logFileWriter.writeToLogFile("=== Request Content ===");
					logFileWriter.writeToLogFile(request.getRequestContent());

					// validate request
					final AssertionError[] requestAssertionErrors = WsdlValidationUtils.validateRequest(request);
					LoggingHelper.logRequestValidation(requestAssertionErrors, logFileWriter, logger);

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
					} else if (submitRequestRunnable.response == null) { // timeout by join operation

						final ServiceStatus status = new ServiceStatus(serviceName, service.getUrl(), State.CRITICAL, 0);

						final String message = "Waiting for the response timed out, timeout: "
								+ WAIT_FOR_RESPONSE_TIMEOUT + " ms";

						status.setMessage(message + LoggingHelper.getLogFileText(logFileWriter, logFilesUrl));
						logger.debug(message);
						logFileWriter.writeToLogFile(message);

						newCache.add(status);
					} else {

						final WsdlResponse response = submitRequestRunnable.response;
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
						final ServiceStatus status = new ServiceStatus(serviceName, service.getUrl());
						status.setState(State.OK);
						status.setResponseTime(response.getTimeTaken());

						// build message
						final StringBuffer stringBuffer = new StringBuffer("Service is working");

						// response validation
						final AssertionError[] responseAssertionErrors = WsdlValidationUtils.validateResponse(response);
						LoggingHelper.logResponseValidation(responseAssertionErrors, logFileWriter, logger);
						if (responseAssertionErrors != null && responseAssertionErrors.length > 0) {
							stringBuffer.append(", but the repsonse is not valid");
							status.setState(State.CRITICAL);
						}

						// SOAP fault test
						if (WsdlValidationUtils.isSoapFault(content, request.getOperation())) {
							stringBuffer.append(", but the response is a SOAP fault");
							logger.debug("The response is a SOAP fault!");
							logFileWriter.writeToLogFile("The response is a SOAP fault!");
							status.setState(State.WARNING);
						}

						stringBuffer.append(responseTimeText);
						stringBuffer.append(LoggingHelper.getLogFileText(logFileWriter, logFilesUrl));

						status.setMessage(stringBuffer.toString());

						newCache.add(status);

						// for testing: assertNotNull(content); assertFalse(content.indexOf("404 Not Found") > 0);
						// TODO compare received response with expected values
					}
				}

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

			// TODO which Exception should lead to an critical state in Nagios?

			logFileWriter.close();
		}

		this.cache = newCache;
	}

	private void handleException(final Exception e, final LogFileWriter logFileWriter, final String serviceName,
			final Service service, final List<ServiceStatus> newCache) {

		final ServiceStatus status = new ServiceStatus(serviceName, service.getUrl(), State.CRITICAL, 0);
		status.setMessage("An error occured: " + e.getMessage()
				+ LoggingHelper.getLogFileText(logFileWriter, logFilesUrl));
		newCache.add(status);

		logFileWriter.writeToLogFile("An error occured: " + e.toString());
		logFileWriter.writeStacktracetoLogFile(e);
	}

	private final static class ImportWsdlRunnable implements Runnable {

		private final WsdlProject wsdlProject;
		private final String url;
		public WsdlInterface wsdlInterface;
		public SoapUIException soapUIException;

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
		public WsdlResponse response;
		public SubmitException submitException;

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
