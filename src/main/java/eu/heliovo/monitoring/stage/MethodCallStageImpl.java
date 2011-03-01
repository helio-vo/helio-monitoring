package eu.heliovo.monitoring.stage;

import static eu.heliovo.monitoring.model.ModelFactory.newStatusDetails;

import java.util.*;
import java.util.concurrent.ExecutorService;

import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;

import com.eviware.soapui.impl.wsdl.*;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;
import com.eviware.soapui.model.testsuite.AssertionError;

import eu.heliovo.monitoring.action.*;
import eu.heliovo.monitoring.logging.*;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.util.WsdlValidationUtils;

/**
 * Please see {@link MethodCallStage}.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class MethodCallStageImpl implements MethodCallStage {

	private static final String LOG_FILE_SUFFIX = "_method-call_";
	private static final boolean TEST_FOR_SOAP_FAULT = false;

	private final LoggingFactory loggingFactory;
	private final String logFilesUrl; // TODO should be moved somewhere in the logging classes
	private final ExecutorService executor;

	@Autowired
	protected MethodCallStageImpl(LoggingFactory loggingFactory,
			@Value("${monitoringService.logUrl}") String logFilesUrl, ExecutorService executor) {

		this.loggingFactory = loggingFactory;
		this.logFilesUrl = logFilesUrl;
		this.executor = executor;
	}

	// TODO cache WSDL documents to avoid the import on every run
	// see http://replay.waybackmachine.org/20081220182936/http://www.soapui.org/architecture/integration.html
	// see http://www.soapui.org/userguide/functional/response-assertions.html, download source of soapUI &
	// search for files containing "assertion"
	@Override
	public List<StatusDetails<Service>> getStatus(Set<Service> services) {

		List<StatusDetails<Service>> servicesStatus = new ArrayList<StatusDetails<Service>>();

		for (Service service : services) {

			String serviceName = service.getName();
			String serviceUrlAsString = service.getUrl().toString();
			String logFileWriterName = service.getCanonicalName() + LOG_FILE_SUFFIX;
			LogFileWriter logFileWriter = loggingFactory.newLogFileWriter(logFileWriterName);
			WsdlInterface wsdlInterface = null;

			try {

				wsdlInterface = new ImportWsdlAction(logFileWriter, serviceUrlAsString, executor).getResult();
				WsdlOperation operation = new SelectOperationAction(wsdlInterface).getResult();
				WsdlRequest request = new CreateRequestAction(wsdlInterface, logFileWriter, operation).getResult();
				WsdlResponse response = new SubmitRequestAction(request, logFileWriter, executor).getResult();
				new ProcessResponseAction(response, serviceName, service, logFileWriter).execute();
				StatusDetails<Service> serviceStatus = buildServiceStatus(response, serviceName, service, logFileWriter);

				servicesStatus.add(serviceStatus);

			} catch (Exception e) {
				new HandleErrorAction(e, logFileWriter, serviceName, service, servicesStatus, logFilesUrl).execute();
			} finally {
				new CleanUpAction(logFileWriter, wsdlInterface).execute();
			}
		}

		return servicesStatus;
	}

	private StatusDetails<Service> buildServiceStatus(WsdlResponse response, String serviceName, Service service,
			LogFileWriter logFileWriter) throws XmlException {

		// build message
		StringBuffer messageBuffer = new StringBuffer();
		Status status = Status.OK;

		if (responseIsNotValid(response, logFileWriter)) {

			messageBuffer.append("Service is malfunctioning, because the repsonse is not valid");
			status = Status.CRITICAL;

		} else {

			messageBuffer.append("Service is working");

			if (TEST_FOR_SOAP_FAULT) {

				WsdlOperation operation = response.getRequest().getOperation();
				if (WsdlValidationUtils.isSoapFault(response.getContentAsString(), operation)) {
					messageBuffer.append(", but the response is a SOAP fault");
					logFileWriter.write("The response is a SOAP fault!");
					status = Status.WARNING;
				}
			}
		}

		long responseTime = response.getTimeTaken();
		messageBuffer.append(", response time = " + responseTime + " ms");
		messageBuffer.append(LoggingHelper.getLogFileText(logFileWriter, logFilesUrl));
		String message = messageBuffer.toString();

		return newStatusDetails(service, serviceName, service.getUrl(), status, responseTime, message);
	}

	private boolean responseIsNotValid(WsdlResponse response, LogFileWriter logFileWriter) {

		// response validation
		AssertionError[] responseAssertionErrors = WsdlValidationUtils.validateResponse(response);
		LoggingHelper.logResponseValidation(responseAssertionErrors, logFileWriter);

		return responseAssertionErrors != null && responseAssertionErrors.length > 0;
	}
}