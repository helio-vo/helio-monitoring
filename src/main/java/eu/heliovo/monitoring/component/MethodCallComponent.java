package eu.heliovo.monitoring.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.impl.wsdl.WsdlSubmitContext;
import com.eviware.soapui.model.iface.Response;
import com.eviware.soapui.model.iface.Request.SubmitException;
import com.eviware.soapui.support.SoapUIException;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.State;
import eu.heliovo.monitoring.util.LogFileWriter;

@Component
public class MethodCallComponent extends AbstractComponent {

	protected Logger logger = Logger.getLogger(this.getClass());
	private final String logFilesDirectory;
	private final String logFilesUrl;

	// TODO enable configuration of these parameters from ressoucre file helio-monitoring.properties
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
	@Override
	public void refreshCache() {

		final List<ServiceStatus> newCache = new ArrayList<ServiceStatus>();

		for (final Service service : super.services) {

			final String serviceName = service.getName() + SERVICE_NAME_SUFFIX;
			final LogFileWriter logFileWriter = new LogFileWriter(logFilesDirectory, service.getName()
					+ LOG_FILE_SUFFIX);

			try {

				// see http://www.soapui.org/architecture/integration.html

				final WsdlProject project = new WsdlProject();

				logger.debug("Importing WSDL file");
				logFileWriter.writeToLogFile("Importing WSDL file");

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

					final WsdlRequest request = operation.addNewRequest(operation.getName() + "Request");

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

						final Response response = submitRequestRunnable.response;
						final long responseTime = response.getTimeTaken();

						final String responseTimeText = ", response time = " + responseTime + " ms";
						logger.debug("Response received" + responseTimeText);
						logFileWriter.writeToLogFile("Response received" + responseTimeText);

						logger.debug("=== Response Content ===");
						logFileWriter.writeToLogFile("=== Response Content ===");

						final String content = response.getContentAsString();
						logger.debug(content);
						logFileWriter.writeToLogFile(content);

						final ServiceStatus status = new ServiceStatus(serviceName, service.getUrl());
						status.setState(State.UP);
						status.setResponseTime(response.getTimeTaken());
						status.setMessage("Service is working" + responseTimeText + getLogFileText(logFileWriter));

						newCache.add(status);

						// for testing: assertNotNull(content); assertFalse(content.indexOf("404 Not Found") > 0);
						// TODO compare received response with expected values
					}
				}
			} catch (final Exception e) {

				final ServiceStatus status = new ServiceStatus(serviceName, service.getUrl());
				status.setMessage("An error occured: " + e.getMessage() + getLogFileText(logFileWriter));
				newCache.add(status);

				logFileWriter.writeToLogFile("An error occured: " + e.toString());
			}
			// TODO which Exception should lead to an critical state in Nagios?

			logFileWriter.close();
		}

		this.cache = newCache;
	}

	private String getLogFileText(final LogFileWriter logFileWriter) {

		final StringBuffer buffer = new StringBuffer();
		buffer.append(", see log file: ");
		buffer.append(logFilesUrl);
		buffer.append("/");
		buffer.append(logFileWriter.getFileName());

		return buffer.toString();
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
		public Response response;
		public SubmitException submitException;

		private final static boolean ASYNC = false;

		public SubmitRequestRunnable(final WsdlRequest request) {
			this.request = request;
		}

		@Override
		public void run() {
			try {
				response = request.submit(new WsdlSubmitContext(null), ASYNC).getResponse();
			} catch (final SubmitException e) {
				submitException = e;
			}
		}

	}
}
