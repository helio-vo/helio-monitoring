package eu.heliovo.monitoring.stage;

import static eu.heliovo.monitoring.model.ModelFactory.newServiceStatusDetails;

import java.util.*;
import java.util.concurrent.ExecutorService;

import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;

import com.eviware.soapui.impl.wsdl.*;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;
import com.eviware.soapui.model.testsuite.AssertionError;

import eu.heliovo.monitoring.action.*;
import eu.heliovo.monitoring.listener.ServiceUpdateListener;
import eu.heliovo.monitoring.logging.*;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.util.WsdlValidationUtils;

/**
 * Just calls one method of every service to see that it is working.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class MethodCallStage implements MonitoringStage, ServiceUpdateListener {

	protected static final String SERVICE_NAME_SUFFIX = " -method call-";
	private static final String LOG_FILE_SUFFIX = "_method-call_";
	private static final boolean TEST_FOR_SOAP_FAULT = false;

	private final StageHelper stageHelper;
	private final LoggingFactory loggingFactory;
	private final String logFilesUrl; // TODO should be moved somewhere in the logging classes
	private final ExecutorService executor;

	private Set<Service> services = Collections.emptySet();
	private List<ServiceStatusDetails> servicesStatus = Collections.emptyList();

	@Autowired
	public MethodCallStage(StageHelper stageHelper, LoggingFactory loggingFactory,
			@Value("${monitoringService.logUrl}") String logFilesUrl, ExecutorService executor) {

		this.stageHelper = stageHelper;
		this.loggingFactory = loggingFactory;
		this.logFilesUrl = logFilesUrl;
		this.executor = executor;
	}

	// TODO cache WSDL documents to avoid the import on every run
	// see http://www.soapui.org/architecture/integration.html
	// see http://www.soapui.org/userguide/functional/response-assertions.html, download source of soapUI &
	// search for files containing "assertion"
	@Override
	public synchronized void updateStatus() {

		List<ServiceStatusDetails> servicesStatus = new ArrayList<ServiceStatusDetails>();

		for (Service service : services) {

			String serviceName = service.getName() + SERVICE_NAME_SUFFIX;
			String serviceUrlAsString = service.getUrl().toString();
			String logFileWriterName = service.getCanonicalName() + LOG_FILE_SUFFIX;
			LogFileWriter logFileWriter = loggingFactory.newLogFileWriter(logFileWriterName);
			WsdlInterface wsdlInterface = null;

			try {

				wsdlInterface = new ImportWsdlAction(logFileWriter, serviceUrlAsString, executor).getResult();
				WsdlOperation operation = new SelectOperationAction(wsdlInterface).getResult();
				WsdlRequest request = stageHelper.createRequest(wsdlInterface, logFileWriter, operation);
				WsdlResponse response = stageHelper.submitRequest(request, logFileWriter);
				stageHelper.processResponse(response, serviceName, service, logFileWriter);
				ServiceStatusDetails serviceStatus = buildServiceStatus(response, serviceName, service, logFileWriter);

				servicesStatus.add(serviceStatus);

			} catch (Exception e) {
				stageHelper.handleException(e, logFileWriter, serviceName, service, servicesStatus);
			} finally {
				stageHelper.cleanUp(logFileWriter, wsdlInterface);
			}
		}
		this.servicesStatus = servicesStatus;
	}

	private ServiceStatusDetails buildServiceStatus(WsdlResponse response, String serviceName, Service service,
			LogFileWriter logFileWriter) throws XmlException {

		// build message
		StringBuffer stringBuffer = new StringBuffer("Service is working");
		ServiceStatus status = ServiceStatus.OK;

		// response validation
		AssertionError[] responseAssertionErrors = WsdlValidationUtils.validateResponse(response);
		LoggingHelper.logResponseValidation(responseAssertionErrors, logFileWriter);

		if (responseAssertionErrors != null && responseAssertionErrors.length > 0) {

			stringBuffer.append(", but the repsonse is not valid");
			status = ServiceStatus.CRITICAL;

		} else if (TEST_FOR_SOAP_FAULT) {

			WsdlOperation operation = response.getRequest().getOperation();
			if (WsdlValidationUtils.isSoapFault(response.getContentAsString(), operation)) {
				stringBuffer.append(", but the response is a SOAP fault");
				logFileWriter.write("The response is a SOAP fault!");
				status = ServiceStatus.WARNING;
			}
		}

		long responseTime = response.getTimeTaken();
		stringBuffer.append(", response time = " + responseTime + " ms");
		stringBuffer.append(LoggingHelper.getLogFileText(logFileWriter, logFilesUrl));
		String message = stringBuffer.toString();

		return newServiceStatusDetails(serviceName, service.getUrl(), status, responseTime, message);
	}

	@Override
	public synchronized List<ServiceStatusDetails> getServicesStatus() {
		return servicesStatus;
	}

	@Override
	public synchronized void updateServices(Set<Service> newServices) {
		this.services = newServices;
	}
}