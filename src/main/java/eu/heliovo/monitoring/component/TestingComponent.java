package eu.heliovo.monitoring.component;

import static eu.heliovo.monitoring.model.ModelFactory.newServiceStatusDetails;
import static eu.heliovo.monitoring.util.ReflectionUtils.implementsInterface;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import java.net.URL;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;

import com.eviware.soapui.impl.wsdl.*;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;
import com.eviware.soapui.model.iface.Operation;
import com.eviware.soapui.model.testsuite.AssertionError;

import eu.heliovo.monitoring.logging.*;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.util.WsdlValidationUtils;

/**
 * Does a full test of all services and their methods.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class TestingComponent implements MonitoringComponent {

	protected static final String SERVICE_NAME_SUFFIX = " -testing-";
	private static final String LOG_FILE_SUFFIX = "_testing_";

	private final ComponentHelper componentHelper;
	private final LoggingFactory loggingFactory;
	private final String logFilesUrl;

	private final Logger logger = Logger.getLogger(this.getClass());

	private List<Service> services = Collections.emptyList();
	private List<ServiceStatusDetails> servicesStatus = Collections.emptyList();

	@Autowired
	public TestingComponent(ComponentHelper componentHelper, LoggingFactory loggingFactory,
			@Value("${monitoringService.logUrl}") String logFilesUrl) {

		this.componentHelper = componentHelper;
		this.loggingFactory = loggingFactory;
		this.logFilesUrl = logFilesUrl;
	}

	@Override
	public synchronized void updateStatus() {

		List<ServiceStatusDetails> newCache = new ArrayList<ServiceStatusDetails>();

		for (Service service : services) {

			String serviceName = service.getName() + SERVICE_NAME_SUFFIX;
			String logFileWriterName = service.getName() + LOG_FILE_SUFFIX;
			LogFileWriter logFileWriter = loggingFactory.newLogFileWriter(logFileWriterName);

			try {

				WsdlInterface wsdlInterface = componentHelper.importWsdl(logFileWriter, service.getUrl().toString());
				Statistic statistic = new Statistic(serviceName, service.getUrl());

				monitorPredefinedOperations(logFileWriter, service, serviceName, wsdlInterface, statistic);
				monitorAvailableOperations(logFileWriter, service, serviceName, wsdlInterface, statistic);

				// TODO record value over a time, if 3 times the same, take as right value, if it changes, do alert
				// => problem if value are generated and data type to less restrictive, e.g. string for an integer

				// TODO if service has predefined testing data, user can decide to deactivate the test of not
				// predefined operations

				ServiceStatusDetails serviceStatusDetails = buildServiceStatusDetails(statistic, logFileWriter);

				newCache.add(serviceStatusDetails);
				wsdlInterface.getProject().release();

			} catch (Exception e) {
				componentHelper.handleException(e, logFileWriter, serviceName, service, newCache);
			}
			logFileWriter.close();
		}
		this.servicesStatus = newCache;
	}

	// TODO refactor, too many nested blocks
	private void monitorPredefinedOperations(LogFileWriter logFileWriter, Service service, String serviceName,
			WsdlInterface wsdlInterface, Statistic statistic) {

		if (implementsInterface(service, TestingService.class)) {

			logFileWriter.write("==== Testing predefined operations ====");

			TestingService testingService = (TestingService) service;
			for (OperationTest operationTest : testingService.getOperationTests()) {

				statistic.predefinedOperationsTested++;

				WsdlOperation operation = wsdlInterface.getOperationByName(operationTest.getOperationName());
				if (operation != null) {

					try {

						WsdlOperation wsdlOperation = operation;

						String requestContent = operationTest.getRequestContent();
						WsdlRequest request;
						if (hasText(requestContent)) {
							request = componentHelper.createRequest(wsdlInterface, logFileWriter, wsdlOperation,
									requestContent);
						} else {
							request = componentHelper.createRequest(wsdlInterface, logFileWriter, wsdlOperation);
						}

						WsdlResponse response = componentHelper.submitRequest(request, logFileWriter);
						processResponse(response, statistic, service, serviceName, logFileWriter);

						String actualResponseContent = response.getContentAsString();
						String expectedResponseContent = operationTest.getResponseContent();

						if (hasText(expectedResponseContent)) {

							XmlObject expectedXml = XmlObject.Factory.parse(expectedResponseContent);
							XmlObject actualXml = XmlObject.Factory.parse(actualResponseContent);

							if (expectedXml.valueEquals(actualXml)) {
								logFileWriter.write("Actual and predefined response are matching");
							} else {

								statistic.notMatchingResponseOperations.add(operation.getName());

								logFileWriter.write("Actual and expected response are not the same!");
								logFileWriter.write("=== Actual response Content for Operation \""
										+ operation.getName() + "\" ===");
								logFileWriter.write(actualResponseContent);

							}
						}

						// TODO if request is a predefined one and not valid => State.CRITICAL, may throw an exeception
						// TODO test with predefined response only

					} catch (Exception e) {
						logException(e, logFileWriter, operation.getName(), statistic);
					}

				} else {

					String operationName = operationTest.getOperationName();
					String message = "predefined operation " + operationName + " not found!";
					logFileWriter.write(message);
					statistic.predefinedNotFoundOperations.add(operationTest.getOperationName());
				}
			}
		}
	}

	private void monitorAvailableOperations(LogFileWriter logFileWriter, Service service, String serviceName,
			WsdlInterface wsdlInterface, Statistic statistic) {

		logFileWriter.write("==== Testing all available operations ====");

		for (Operation operation : wsdlInterface.getOperationList()) {

			if (operation instanceof WsdlOperation) {

				statistic.operationsTested++;

				try {

					WsdlOperation wsdlOperation = (WsdlOperation) operation;
					WsdlRequest request = componentHelper.createRequest(wsdlInterface, logFileWriter, wsdlOperation);
					WsdlResponse response = componentHelper.submitRequest(request, logFileWriter);
					processResponse(response, statistic, service, serviceName, logFileWriter);

				} catch (Exception e) {
					logException(e, logFileWriter, operation.getName(), statistic);
				}

			} else {
				logger.warn("operation " + operation.getName() + " is not a WSDL operation!");
			}

		}
	}

	private void processResponse(WsdlResponse response, Statistic statistic, Service service, String serviceName,
			LogFileWriter logFileWriter) throws XmlException {

		componentHelper.processResponse(response, serviceName, service, logFileWriter);

		// response validation
		WsdlOperation operation = response.getRequest().getOperation();
		AssertionError[] responseAssertionErrors = WsdlValidationUtils.validateResponse(response);
		LoggingHelper.logResponseValidation(responseAssertionErrors, logFileWriter);

		if (responseAssertionErrors != null && responseAssertionErrors.length > 0) {
			statistic.invalidResponseOperations.add(operation.getName());
		} else if (WsdlValidationUtils.isSoapFault(response.getContentAsString(), operation)) {
			// SOAP fault test
			statistic.soapFaultOperations.add(operation.getName());
			logFileWriter.write("The response is a SOAP fault!");
		}
	}

	// TODO refactor, to many nested blocks
	private ServiceStatusDetails buildServiceStatusDetails(Statistic statistic, LogFileWriter logFileWriter) {

		// assemble statistic message
		StringBuffer message = new StringBuffer("Testing Result: ");

		int problems = statistic.exceptionalOperations.size() + statistic.predefinedNotFoundOperations.size()
				+ statistic.invalidResponseOperations.size() + statistic.notMatchingResponseOperations.size();

		int operationsTotal = statistic.operationsTested + statistic.predefinedOperationsTested;
		int successful = operationsTotal - problems;

		message.append(successful);
		message.append("/");
		message.append(operationsTotal);
		message.append(" tests successful");

		if (problems > 0) {

			message.append(" (");

			if (statistic.exceptionalOperations.size() > 0) {
				message.append(message.charAt(message.length() - 1) != '(' ? ", " : "");
				message.append(statistic.exceptionalOperations.size());
				message.append(" exceptions");
			}
			if (statistic.invalidResponseOperations.size() > 0) {
				message.append(message.charAt(message.length() - 1) != '(' ? ", " : "");
				message.append(statistic.invalidResponseOperations.size());
				message.append(" invalid responses");
			}
			if (statistic.predefinedNotFoundOperations.size() > 0) {
				message.append(message.charAt(message.length() - 1) != '(' ? ", " : "");
				message.append(statistic.predefinedNotFoundOperations.size());
				message.append(" operations predefined but not found");
			}
			if (statistic.notMatchingResponseOperations.size() > 0) {
				message.append(message.charAt(message.length() - 1) != '(' ? ", " : "");
				message.append(statistic.notMatchingResponseOperations.size());
				message.append(" operations with predefined response not matching");
			}

			message.append(")");
		}

		if (statistic.soapFaultOperations.size() > 0) {
			message.append(", there are ");
			message.append(statistic.soapFaultOperations.size());
			message.append(" SOAP faults");
		}

		// TODO test notMatchingResponseOperations (request + response, response only)

		String logMessage = message.toString();
		logStatistic(statistic, logFileWriter, logMessage);

		message.append(LoggingHelper.getLogFileText(logFileWriter, logFilesUrl));
		String statusMessage = message.toString();

		ServiceStatus status = statistic.getServiceStatus();
		return newServiceStatusDetails(statistic.serviceName, statistic.serviceUrl, status, 0, statusMessage);
	}

	// TODO refactor, to many nested blocks
	private void logStatistic(Statistic statistic, LogFileWriter logFileWriter, String logMessage) {

		logFileWriter.write("==== Testing statistic ====");
		logFileWriter.write(logMessage);

		if (!isEmpty(statistic.exceptionalOperations)) {
			logFileWriter.write("=== exceptional operations ===");
			for (String operationName : statistic.exceptionalOperations) {
				logFileWriter.write(operationName);
			}
		}

		if (!isEmpty(statistic.invalidResponseOperations)) {
			logFileWriter.write("=== operations with invalid responses ===");
			for (String operationName : statistic.invalidResponseOperations) {
				logFileWriter.write(operationName);
			}
		}

		if (!isEmpty(statistic.predefinedNotFoundOperations)) {
			logFileWriter.write("=== predefined operations not found ===");
			for (String operationName : statistic.predefinedNotFoundOperations) {
				logFileWriter.write(operationName);
			}
		}

		if (!isEmpty(statistic.soapFaultOperations)) {
			logFileWriter.write("=== operations with SOAP faults ===");
			for (String operationName : statistic.soapFaultOperations) {
				logFileWriter.write(operationName);
			}
		}

		if (!isEmpty(statistic.notMatchingResponseOperations)) {
			logFileWriter.write("=== operations with reponse not matching with predefinition ===");
			for (String operationName : statistic.notMatchingResponseOperations) {
				logFileWriter.write(operationName);
			}
		}
	}

	private void logException(Exception exception, LogFileWriter logFileWriter, String operationName,
			Statistic statistic) {

		statistic.exceptionalOperations.add(operationName);
		logFileWriter.write(exception);
	}

	// TODO invalid requests
	// at least one soap fault => WARNING state
	// at least one invalid response => CRITICAL state
	// invalid predefined request => WARNING or CRITICAL?
	// predefined: what when predefined request or predefined repsonse invalid?
	// predefined: CRITICAL when response does not match or if no response predefined and response is
	// predefined: operation with predefined name not found => CRITICAL
	// invalid or SOAP fault => CRITICAL, because user did predefine, he means the result must be valid
	// maybe separate predefined statistic

	private static class Statistic {

		private final String serviceName;
		private final URL serviceUrl;

		private int operationsTested = 0;
		private int predefinedOperationsTested = 0;

		private final List<String> soapFaultOperations = new ArrayList<String>();
		private final List<String> invalidResponseOperations = new ArrayList<String>();
		private final List<String> exceptionalOperations = new ArrayList<String>();
		private final List<String> predefinedNotFoundOperations = new ArrayList<String>();
		private final List<String> notMatchingResponseOperations = new ArrayList<String>();

		public Statistic(String serviceName, URL serviceUrl) {
			this.serviceName = serviceName;
			this.serviceUrl = serviceUrl;
		}

		public ServiceStatus getServiceStatus() {

			ServiceStatus status = ServiceStatus.OK;
			if (!isEmpty(soapFaultOperations)) {
				status = ServiceStatus.WARNING;
			}
			if (!isEmpty(exceptionalOperations)) {
				status = ServiceStatus.CRITICAL;
			}
			if (!isEmpty(predefinedNotFoundOperations)) {
				status = ServiceStatus.CRITICAL;
			}
			if (!isEmpty(invalidResponseOperations)) {
				status = ServiceStatus.CRITICAL;
			}
			if (!isEmpty(notMatchingResponseOperations)) {
				status = ServiceStatus.CRITICAL;
			}
			return status;
		}
	}

	@Override
	public synchronized void setServices(List<Service> services) {
		this.services = services;
	}

	@Override
	public List<ServiceStatusDetails> getServicesStatus() {
		return servicesStatus;
	}
}