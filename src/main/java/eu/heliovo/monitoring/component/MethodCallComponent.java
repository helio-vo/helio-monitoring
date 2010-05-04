package eu.heliovo.monitoring.component;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.impl.wsdl.WsdlSubmitContext;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;
import com.eviware.soapui.impl.wsdl.support.soap.SoapUtils;
import com.eviware.soapui.impl.wsdl.support.soap.SoapVersion;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlContext;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlValidator;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlResponseMessageExchange;
import com.eviware.soapui.model.iface.Request.SubmitException;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.support.SoapUIException;

import eu.heliovo.monitoring.logging.DummyLogFileWriter;
import eu.heliovo.monitoring.logging.LogFileWriter;
import eu.heliovo.monitoring.logging.LogFileWriterImpl;
import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.State;

@Component
public class MethodCallComponent extends AbstractComponent {

	protected Logger logger = Logger.getLogger(this.getClass());
	private final String logFilesDirectory;
	private final String logFilesUrl;
	private final FilenameFilter logFilenameFilter = new LogFilenameFilter();
	private final LogFileWriter dummyLogFileWriter = new DummyLogFileWriter();

	// TODO enable configuration of these parameters from resource file helio-monitoring.properties
	private final static int IMPORT_WSDL_TIMEOUT = 4000; // in milliseconds
	private final static int WAIT_FOR_RESPONSE_TIMEOUT = 10000; // in milliseconds
	private final static boolean GENERATE_OPTINAL_PARAMS = true;
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

		deleteLogFilesOlderThanOneDay();

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

					final ServiceStatus status = new ServiceStatus(serviceName, service.getUrl(), State.DOWN, 0);

					final String message = "Importing the WSDL file timed out, timeout: " + IMPORT_WSDL_TIMEOUT + " ms";
					status.setMessage(message + getLogFileText(logFileWriter));
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

						final ServiceStatus status = new ServiceStatus(serviceName, service.getUrl(), State.DOWN, 0);

						final String message = "Waiting for the response timed out, timeout: "
								+ WAIT_FOR_RESPONSE_TIMEOUT + " ms";

						status.setMessage(message + getLogFileText(logFileWriter));
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
						status.setState(State.UP);
						status.setResponseTime(response.getTimeTaken());

						// build message
						final StringBuffer stringBuffer = new StringBuffer("Service is working");

						// response validation
						final AssertionError[] assertionErrors = validateResponse(response);
						logResponseValidation(assertionErrors, logFileWriter, logger);
						if (assertionErrors != null && assertionErrors.length > 0) {
							stringBuffer.append(", but the repsonse is not valid");
						}

						// SOAP fault test
						if (isSoapFault(content, request.getOperation())) {
							stringBuffer.append(", but the response is a SOAP fault");
							logger.debug("The response is a SOAP fault!");
							logFileWriter.writeToLogFile("The response is a SOAP fault!");
						}

						stringBuffer.append(responseTimeText);
						stringBuffer.append(getLogFileText(logFileWriter));

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

	private boolean isSoapFault(final String responseContent, final WsdlOperation operation) throws XmlException {
		final SoapVersion soapVersion = operation.getInterface().getSoapVersion();
		return SoapUtils.isSoapFault(responseContent, soapVersion);
	}

	private AssertionError[] validateResponse(final WsdlResponse response) {

		final WsdlRequest request = response.getRequest();
		final WsdlContext wsdlContext = request.getOperation().getInterface().getWsdlContext();
		final WsdlValidator validator = new WsdlValidator(wsdlContext);

		final WsdlResponseMessageExchange wsdlMessageExchange = new WsdlResponseMessageExchange(request);
		wsdlMessageExchange.setResponse(response);

		return validator.assertResponse(wsdlMessageExchange, true);
	}

	private void logResponseValidation(final AssertionError[] assertionErrors, final LogFileWriter logFileWriter,
			final Logger logger) {

		logger.debug("Validating response");
		logFileWriter.writeToLogFile("Validating response");

		if (assertionErrors == null || assertionErrors.length < 1) {
			logger.debug("No validation erros found");
			logFileWriter.writeToLogFile("No validation erros found");
		} else {

			logger.debug("The response has validation erros:");
			logFileWriter.writeToLogFile("The response has validation erros:");

			for (int i = 0; i < assertionErrors.length; i++) {
				logger.debug(assertionErrors[i].toString());
				logFileWriter.writeToLogFile(assertionErrors[i].toString());
			}
		}

	}

	private void handleException(final Exception e, final LogFileWriter logFileWriter, final String serviceName,
			final Service service, final List<ServiceStatus> newCache) {

		final ServiceStatus status = new ServiceStatus(serviceName, service.getUrl(), State.DOWN, 0);
		status.setMessage("An error occured: " + e.getMessage() + getLogFileText(logFileWriter));
		newCache.add(status);

		logFileWriter.writeToLogFile("An error occured: " + e.toString());
		logFileWriter.writeStacktracetoLogFile(e);
	}

	private void deleteLogFilesOlderThanOneDay() {

		final File[] oldLogFiles = new File(logFilesDirectory).listFiles(logFilenameFilter);
		final Calendar now = Calendar.getInstance();

		if (oldLogFiles != null) {
			for (int i = 0; i < oldLogFiles.length; i++) {
				if (isDifferenceGreaterThanSevenDays(now.getTimeInMillis(), oldLogFiles[i].lastModified())) {
					try {
						oldLogFiles[i].delete();
					} catch (final SecurityException e) {
						logger.info("log file " + oldLogFiles[i].toString() + " could not be deleted", e);
					}
				}
			}
		}
	}

	private static boolean isDifferenceGreaterThanSevenDays(final long milliseconds1, final long milliseconds2) {
		return Math.abs(calculateDiffernceInDays(milliseconds1, milliseconds2)) > 7;
	}

	private static long calculateDiffernceInDays(final long milliseconds1, final long milliseconds2) {
		return (milliseconds1 - milliseconds2) / (1000 * 60 * 60 * 24);
	}

	private String getLogFileText(final LogFileWriter logFileWriter) {

		if (logFileWriter == dummyLogFileWriter) {
			return "";
		}

		final StringBuffer buffer = new StringBuffer();
		buffer.append(", see log file: ");
		buffer.append(logFilesUrl);
		buffer.append("/");
		buffer.append(logFileWriter.getFileName());

		return buffer.toString();
	}

	private final static class LogFilenameFilter implements FilenameFilter {

		@Override
		public boolean accept(final File dir, final String name) {
			return name.endsWith(LogFileWriter.FILE_SUFFIX);
		}
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
				wsdlInterface = wsdlProject.importWsdl(url, true)[0];
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
