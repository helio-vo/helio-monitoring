package eu.heliovo.monitoring.stage;

import static eu.heliovo.monitoring.model.ModelFactory.newStatusDetails;
import static eu.heliovo.monitoring.util.ReflectionUtils.implementsInterface;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;

import com.eviware.soapui.impl.wsdl.*;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;
import com.eviware.soapui.model.iface.Operation;
import com.eviware.soapui.model.testsuite.AssertionError;

import eu.heliovo.monitoring.action.*;
import eu.heliovo.monitoring.logging.*;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.util.WsdlValidationUtils;

/**
 * Please see {@link TestingStage}.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class TestingStageImpl implements TestingStage {

	private static final String LOG_FILE_SUFFIX = "_testing_";

	private final LoggingFactory loggingFactory;
	private final String logFilesUrl;
	private final ExecutorService executor;

	private final Logger logger = Logger.getLogger(this.getClass());

	@Autowired
	protected TestingStageImpl(LoggingFactory loggingFactory, @Value("${monitoringService.logUrl}") String logFilesUrl,
			ExecutorService executor) {

		this.loggingFactory = loggingFactory;
		this.logFilesUrl = logFilesUrl;
		this.executor = executor;
	}

	@Override
	public List<StatusDetails<Service>> getStatus(Set<Service> services) {

		List<StatusDetails<Service>> statusDetails = new ArrayList<StatusDetails<Service>>();

		for (Service service : services) {

			String serviceName = service.getName();
			String logFileWriterName = service.getCanonicalName() + LOG_FILE_SUFFIX;
			LogFileWriter logFileWriter = loggingFactory.newLogFileWriter(logFileWriterName);
			WsdlInterface wsdlInterface = null;

			try {

				String wsdlUrl = service.getUrl().toString();
				wsdlInterface = new ImportWsdlAction(logFileWriter, wsdlUrl, executor).getResult();

				Statistic statistic = new Statistic(serviceName, service.getUrl());

				monitorPredefinedOperations(logFileWriter, service, serviceName, wsdlInterface, statistic);
				monitorAvailableOperations(logFileWriter, service, serviceName, wsdlInterface, statistic);

				StatusDetails<Service> serviceStatusDetails;
				serviceStatusDetails = buildServiceStatusDetails(service, statistic, logFileWriter);
				statusDetails.add(serviceStatusDetails);

			} catch (Exception e) {
				new HandleErrorAction(e, logFileWriter, serviceName, service, statusDetails, logFilesUrl).execute();
			} finally {
				new CleanUpAction(logFileWriter, wsdlInterface).execute();
			}
		}
		return statusDetails;
	}

	private void monitorPredefinedOperations(LogFileWriter logFileWriter, Service service, String serviceName,
			WsdlInterface wsdlInterface, Statistic statistic) {

		if (implementsInterface(service, TestingService.class)) {

			logFileWriter.write("==== Testing predefined operations ====");

			TestingService testingService = (TestingService) service;
			for (OperationTest operationTest : testingService.getOperationTests()) {

				statistic.predefinedOperationsTested++;

				WsdlOperation operation = wsdlInterface.getOperationByName(operationTest.getOperationName());
				if (operation != null) {

					monitorPredefinedOperation(logFileWriter, service, serviceName, statistic, operationTest, operation);
				} else {
					handlePredefinedOperationNotFound(logFileWriter, statistic, operationTest);
				}
			}
		}
	}

	private void monitorPredefinedOperation(LogFileWriter logFileWriter, Service service, String serviceName,
			Statistic statistic, OperationTest operationTest, WsdlOperation operation) {

		try {

			WsdlOperation wsdlOperation = operation;
			String content = operationTest.getRequestContent();
			WsdlRequest request;

			if (hasText(content)) {

				CreateRequestAction action;
				action = new CreateRequestAction(logFileWriter, wsdlOperation, content);
				request = action.getResult();

			} else {
				request = new CreateRequestAction(logFileWriter, wsdlOperation).getResult();
			}

			WsdlResponse response = new SubmitRequestAction(request, logFileWriter, executor).getResult();
			processResponse(response, statistic, logFileWriter);
			evaluateResponse(logFileWriter, statistic, operationTest, operation, response);

			// TODO test with predefined response only

		} catch (Exception e) {
			logException(e, logFileWriter, operation.getName(), statistic);
		}
	}

	private void evaluateResponse(LogFileWriter logFileWriter, Statistic statistic, OperationTest operationTest,
			WsdlOperation operation, WsdlResponse response) throws XmlException {

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
				logFileWriter.write("=== Actual response Content for Operation \"" + operation.getName() + "\" ===");
				logFileWriter.write(actualResponseContent);
			}
		}
	}

	private void handlePredefinedOperationNotFound(LogFileWriter logFileWriter, Statistic statistic,
			OperationTest operationTest) {

		String operationName = operationTest.getOperationName();
		String message = "predefined operation " + operationName + " not found!";
		logFileWriter.write(message);
		statistic.predefinedNotFoundOperations.add(operationTest.getOperationName());
	}

	private void monitorAvailableOperations(LogFileWriter logFileWriter, Service service, String serviceName,
			WsdlInterface wsdlInterface, Statistic statistic) {

		logFileWriter.write("==== Testing all available operations ====");

		for (Operation operation : wsdlInterface.getOperationList()) {

			if (operation instanceof WsdlOperation) {

				statistic.operationsTested++;

				try {

					WsdlOperation wsdlOperation = (WsdlOperation) operation;
					WsdlRequest request = new CreateRequestAction(logFileWriter, wsdlOperation).getResult();
					WsdlResponse response = new SubmitRequestAction(request, logFileWriter, executor).getResult();
					processResponse(response, statistic, logFileWriter);

				} catch (Exception e) {
					logException(e, logFileWriter, operation.getName(), statistic);
				}

			} else {
				logger.warn("operation " + operation.getName() + " is not a WSDL operation!");
			}

		}
	}

	private void processResponse(WsdlResponse response, Statistic statistic, LogFileWriter logFileWriter)
			throws XmlException {

		new ProcessResponseAction(response, logFileWriter).execute();

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

		statistic.addResponseTime(response.getTimeTaken());
	}

	// TODO refactor, to many nested blocks
	private StatusDetails<Service> buildServiceStatusDetails(Service service, Statistic statistic,
			LogFileWriter logFileWriter) {

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

		long responseTime = statistic.getAverageResonseTime();
		message.append(", average response time = " + responseTime + " ms");

		message.append(LoggingHelper.getLogFileText(logFileWriter, logFilesUrl));

		String serviceName = statistic.serviceName;
		URL serviceURL = statistic.serviceUrl;
		String statusMessage = message.toString();
		Status status = statistic.getServiceStatus();

		return newStatusDetails(service, serviceName, serviceURL, status, responseTime, statusMessage);
	}

	private void logStatistic(Statistic statistic, LogFileWriter logFileWriter, String logMessage) {

		logFileWriter.write("==== Testing statistic ====");
		logFileWriter.write(logMessage);

		logExceptionalOperations(statistic, logFileWriter);
		logInvalidResponseOperations(statistic, logFileWriter);
		logPredefinedNotFoundOperations(statistic, logFileWriter);
		logSoapFaultOperations(statistic, logFileWriter);
		logNotMatchingResponseOperations(statistic, logFileWriter);
	}

	private void logExceptionalOperations(Statistic statistic, LogFileWriter logFileWriter) {
		if (!isEmpty(statistic.exceptionalOperations)) {
			logFileWriter.write("=== exceptional operations ===");
			for (String operationName : statistic.exceptionalOperations) {
				logFileWriter.write(operationName);
			}
		}
	}

	private void logInvalidResponseOperations(Statistic statistic, LogFileWriter logFileWriter) {
		if (!isEmpty(statistic.invalidResponseOperations)) {
			logFileWriter.write("=== operations with invalid responses ===");
			for (String operationName : statistic.invalidResponseOperations) {
				logFileWriter.write(operationName);
			}
		}
	}

	private void logPredefinedNotFoundOperations(Statistic statistic, LogFileWriter logFileWriter) {
		if (!isEmpty(statistic.predefinedNotFoundOperations)) {
			logFileWriter.write("=== predefined operations not found ===");
			for (String operationName : statistic.predefinedNotFoundOperations) {
				logFileWriter.write(operationName);
			}
		}
	}

	private void logSoapFaultOperations(Statistic statistic, LogFileWriter logFileWriter) {
		if (!isEmpty(statistic.soapFaultOperations)) {
			logFileWriter.write("=== operations with SOAP faults ===");
			for (String operationName : statistic.soapFaultOperations) {
				logFileWriter.write(operationName);
			}
		}
	}

	private void logNotMatchingResponseOperations(Statistic statistic, LogFileWriter logFileWriter) {
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

		private final DescriptiveStatistics responseTimeStatistic = new DescriptiveStatistics();

		public Statistic(String serviceName, URL serviceUrl) {
			this.serviceName = serviceName;
			this.serviceUrl = serviceUrl;
		}

		public Status getServiceStatus() {

			Status status = Status.OK;
			if (!isEmpty(soapFaultOperations)) {
				status = Status.WARNING;
			}
			if (!isEmpty(exceptionalOperations)) {
				status = Status.CRITICAL;
			}
			if (!isEmpty(predefinedNotFoundOperations)) {
				status = Status.CRITICAL;
			}
			if (!isEmpty(invalidResponseOperations)) {
				status = Status.CRITICAL;
			}
			if (!isEmpty(notMatchingResponseOperations)) {
				status = Status.CRITICAL;
			}
			return status;
		}

		public void addResponseTime(long responseTime) {
			responseTimeStatistic.addValue(responseTime);
		}

		public long getAverageResonseTime() {
			return Math.round(responseTimeStatistic.getMean());
		}
	}
}