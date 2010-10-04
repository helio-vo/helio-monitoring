package eu.heliovo.monitoring.component;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

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

import eu.heliovo.monitoring.logging.LogFileWriter;
import eu.heliovo.monitoring.logging.LoggingHelper;
import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.State;
import eu.heliovo.monitoring.util.WsdlValidationUtils;

public final class ComponentHelper {

	private static final Logger logger = Logger.getLogger(ComponentHelper.class);

	// TODO enable configuration of these parameters from resource file helio-monitoring.properties
	private static final int IMPORT_WSDL_TIMEOUT = 10000; // in milliseconds
	private static final boolean GENERATE_OPTINAL_PARAMS = false;
	private static final int WAIT_FOR_RESPONSE_TIMEOUT = 10000; // in milliseconds
	private static String logFilesUrl = "";

	private ComponentHelper() {
	}

	protected static WsdlInterface importWsdl(final LogFileWriter logFileWriter, final String wsdlUrl)
			throws IOException, SoapUIException, XmlException, InterruptedException {

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

	protected static WsdlRequest createRequest(final WsdlInterface wsdlInterface, final LogFileWriter logFileWriter,
			final WsdlOperation operation) {

		final String requestContent = operation.createRequest(GENERATE_OPTINAL_PARAMS);
		return ComponentHelper.createRequest(wsdlInterface, logFileWriter, operation, requestContent);
	}

	protected static WsdlRequest createRequest(final WsdlInterface wsdlInterface, final LogFileWriter logFileWriter,
			final WsdlOperation operation, final String requestContent) {

		final WsdlRequest request = operation.addNewRequest(operation.getName());

		// generate the request content from the schema
		request.setRequestContent(requestContent);

		logger.debug("=== Request Content for Operation \"" + operation.getName() + "\" ===");
		logger.debug(request.getRequestContent());
		logFileWriter.writeToLogFile("=== Request Content for Operation \"" + operation.getName() + "\" ===");
		logFileWriter.writeToLogFile(request.getRequestContent());

		// validate request
		final AssertionError[] requestAssertionErrors = WsdlValidationUtils.validateRequest(request);
		LoggingHelper.logRequestValidation(requestAssertionErrors, logFileWriter);

		return request;
	}

	protected static WsdlResponse submitRequest(final WsdlRequest request, final LogFileWriter logFileWriter)
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

	protected static void processResponse(final WsdlResponse response, final String serviceName, final Service service,
			final LogFileWriter logFileWriter) throws XmlException {

		final long responseTime = response.getTimeTaken();

		final String responseTimeText = ", response time = " + responseTime + " ms";
		logger.debug("Response received" + responseTimeText);
		logFileWriter.writeToLogFile("Response received" + responseTimeText);

		final WsdlOperation operation = response.getRequest().getOperation();
		logger.debug("=== Response Content for Operation \"" + operation.getName() + "\" ===");
		logFileWriter.writeToLogFile("=== Response Content for Operation \"" + operation.getName() + "\" ===");

		final String content = response.getContentAsString();
		logger.debug(content);
		logFileWriter.writeToLogFile(content);
	}

	protected static void handleException(final Exception e, final LogFileWriter logFileWriter,
			final String serviceName, final Service service, final List<ServiceStatus> newCache) {

		final String message = "An error occured: " + e.getMessage()
				+ LoggingHelper.getLogFileText(logFileWriter, logFilesUrl);
		final ServiceStatus status = new ServiceStatus(serviceName, service.getUrl(), State.CRITICAL, 0, message);
		newCache.add(status);

		logFileWriter.writeToLogFile("An error occured: " + e.getMessage());
		logFileWriter.writeStacktracetoLogFile(e);
	}

	public static final void setLogFilesUrl(final String logFilesUrl) {
		ComponentHelper.logFilesUrl = logFilesUrl;
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
