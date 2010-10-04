package eu.heliovo.monitoring.component;

import static eu.heliovo.monitoring.component.ComponentHelper.createRequest;
import static eu.heliovo.monitoring.component.ComponentHelper.handleException;
import static eu.heliovo.monitoring.component.ComponentHelper.importWsdl;
import static eu.heliovo.monitoring.component.ComponentHelper.processResponse;
import static eu.heliovo.monitoring.component.ComponentHelper.submitRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;
import com.eviware.soapui.model.iface.Request.SubmitException;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.support.SoapUIException;

import eu.heliovo.monitoring.logging.LogFileWriter;
import eu.heliovo.monitoring.logging.LoggingFactory;
import eu.heliovo.monitoring.logging.LoggingHelper;
import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.State;
import eu.heliovo.monitoring.util.WsdlValidationUtils;

/**
 * Just calls a random method of every service to see that it is working.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class MethodCallComponent extends AbstractComponent {

	private final Logger logger = Logger.getLogger(this.getClass());
	private final String logFilesDirectory;
	private final String logFilesUrl;

	private static final String LOG_FILE_SUFFIX = "_method-call_";
	private static final boolean TEST_FOR_SOAP_FAULT = false;

	@Autowired
	public MethodCallComponent(@Value("${methodCall.log.filePath}") final String logFilesDirectory,
			@Value("${monitoringService.logUrl}") final String logFilesUrl) {

		super(" -method call-");

		this.logFilesDirectory = logFilesDirectory;
		this.logFilesUrl = logFilesUrl;

		// TODO find a better solution
		ComponentHelper.setLogFilesUrl(logFilesUrl);
	}

	// TODO cache WSDL documents to avoid the import on every run
	// TODO use ExecuterService for concurrency
	// see http://www.soapui.org/architecture/integration.html
	// see http://www.soapui.org/userguide/functional/response-assertions.html, download source of soapUI &
	// search for files containing "assertion"
	@Override
	public void refreshCache() {

		// TODO do this task by scheduling
		LoggingHelper.deleteLogFilesOlderThanOneDay(logFilesDirectory);

		final List<ServiceStatus> newCache = new ArrayList<ServiceStatus>();

		for (final Service service : super.getServices()) {

			final String serviceName = service.getName() + super.getServiceNameSuffix();
			final String logFileWriterName = service.getName() + LOG_FILE_SUFFIX;
			final LogFileWriter logFileWriter = LoggingFactory.newLogFileWriter(logFilesDirectory, logFileWriterName);

			try {

				final WsdlInterface wsdlInterface = importWsdl(logFileWriter, service.getUrl().toString());
				final WsdlOperation operation = selectOperation(wsdlInterface);
				final WsdlRequest request = createRequest(wsdlInterface, logFileWriter, operation);
				final WsdlResponse response = submitRequest(request, logFileWriter);
				processResponse(response, serviceName, service, logFileWriter);
				final ServiceStatus serviceStatus = buildServiceStatus(response, serviceName, service, logFileWriter);

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

	private WsdlOperation selectOperation(final WsdlInterface wsdlInterface) {
		return wsdlInterface.getOperationAt(0);
		// TODO test best strategy + error handling (choose first, a random or all method(s) of wsdl file)
		// wsdlInterface.getOperationCount();
		// wsdlInterface.getOperationList();
	}

	private ServiceStatus buildServiceStatus(final WsdlResponse response, final String serviceName,
			final Service service, final LogFileWriter logFileWriter) throws XmlException {

		// build message
		final StringBuffer stringBuffer = new StringBuffer("Service is working");
		State state = State.OK;

		// response validation
		final AssertionError[] responseAssertionErrors = WsdlValidationUtils.validateResponse(response);
		LoggingHelper.logResponseValidation(responseAssertionErrors, logFileWriter);

		if (responseAssertionErrors != null && responseAssertionErrors.length > 0) {

			stringBuffer.append(", but the repsonse is not valid");
			state = State.CRITICAL;

		} else if (TEST_FOR_SOAP_FAULT) {

			final WsdlOperation operation = response.getRequest().getOperation();
			if (WsdlValidationUtils.isSoapFault(response.getContentAsString(), operation)) {
				stringBuffer.append(", but the response is a SOAP fault");
				logger.debug("The response is a SOAP fault!");
				logFileWriter.writeToLogFile("The response is a SOAP fault!");
				state = State.WARNING;
			}
		}

		final long responseTime = response.getTimeTaken();
		stringBuffer.append(", response time = " + responseTime + " ms");
		stringBuffer.append(LoggingHelper.getLogFileText(logFileWriter, logFilesUrl));
		final String message = stringBuffer.toString();

		return new ServiceStatus(serviceName, service.getUrl(), state, responseTime, message);
	}
}
