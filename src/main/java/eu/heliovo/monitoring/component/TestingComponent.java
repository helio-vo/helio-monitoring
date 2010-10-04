package eu.heliovo.monitoring.component;

import static eu.heliovo.monitoring.component.ComponentHelper.createRequest;
import static eu.heliovo.monitoring.component.ComponentHelper.handleException;
import static eu.heliovo.monitoring.component.ComponentHelper.importWsdl;
import static eu.heliovo.monitoring.component.ComponentHelper.submitRequest;
import static eu.heliovo.monitoring.util.ReflectionUtils.implementsInterface;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;
import com.eviware.soapui.model.iface.Operation;
import com.eviware.soapui.model.iface.Request.SubmitException;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.support.SoapUIException;

import eu.heliovo.monitoring.logging.LogFileWriter;
import eu.heliovo.monitoring.logging.LoggingFactory;
import eu.heliovo.monitoring.logging.LoggingHelper;
import eu.heliovo.monitoring.model.OperationTest;
import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.State;
import eu.heliovo.monitoring.model.TestingService;
import eu.heliovo.monitoring.util.WsdlValidationUtils;

/**
 * Does a full test of all services and their methods.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class TestingComponent extends AbstractComponent {

	private final Logger logger = Logger.getLogger(this.getClass());
	private final String logFilesDirectory;
	private final String logFilesUrl;

	private static final String LOG_FILE_SUFFIX = "_testing_";

	@Autowired
	public TestingComponent(@Value("${methodCall.log.filePath}") final String logFilesDirectory,
			@Value("${monitoringService.logUrl}") final String logFilesUrl) {

		super(" -testing-");
		this.logFilesDirectory = logFilesDirectory;
		this.logFilesUrl = logFilesUrl;

		// TODO find a better solution
		ComponentHelper.setLogFilesUrl(logFilesUrl);
	}

	@Override
	public void refreshCache() {

		final List<ServiceStatus> newCache = new ArrayList<ServiceStatus>();

		for (final Service service : super.getServices()) {

			final String serviceName = service.getName() + super.getServiceNameSuffix();
			final String logFileWriterName = service.getName() + LOG_FILE_SUFFIX;
			final LogFileWriter logFileWriter = LoggingFactory.newLogFileWriter(logFilesDirectory, logFileWriterName);

			try {

				final WsdlInterface wsdlInterface = importWsdl(logFileWriter, service.getUrl().toString());
				final Statistic statistic = new Statistic(serviceName, service.getUrl());

				monitorPredefinedOperations(logFileWriter, service, serviceName, wsdlInterface, statistic);
				monitorAvailableOperations(logFileWriter, service, serviceName, wsdlInterface, statistic);

				// TODO record value over a time, if 3 times the same, take as right value, if it changes, do alert
				// => problem if value are generated and data type to less restrictive, e.g. string for an integer

				// TODO if service has predefined testing data, user can decide to deactivate the test of not
				// predefined operations

				final ServiceStatus serviceStatus = buildServiceStatus(statistic, logFileWriter);

				newCache.add(serviceStatus);
				wsdlInterface.getProject().release();

			} catch (final RuntimeException e) {
				handleException(e, logFileWriter, serviceName, service, newCache);
			} catch (final IOException e) {
				handleException(e, logFileWriter, serviceName, service, newCache);
			} catch (final SoapUIException e) {
				handleException(e, logFileWriter, serviceName, service, newCache);
			} catch (final XmlException e) {
				handleException(e, logFileWriter, serviceName, service, newCache);
			} catch (final InterruptedException e) {
				handleException(e, logFileWriter, serviceName, service, newCache);
			}

			logFileWriter.close();
		}

		super.setCache(newCache);
	}

	private void monitorPredefinedOperations(final LogFileWriter logFileWriter, final Service service,
			final String serviceName, final WsdlInterface wsdlInterface, final Statistic statistic) {

		if (implementsInterface(service, TestingService.class)) {

			logFileWriter.writeToLogFile("==== Testing predefined operations ====");

			final TestingService testingService = (TestingService) service;
			for (final OperationTest operationTest : testingService.getOperationTests()) {

				statistic.predefinedOperationsTested++;

				final WsdlOperation operation = wsdlInterface.getOperationByName(operationTest.getOperationName());
				if (operation != null) {

					try {

						final WsdlOperation wsdlOperation = operation;

						final String requestContent = operationTest.getRequestContent();
						final WsdlRequest request;
						if (hasText(requestContent)) {
							request = createRequest(wsdlInterface, logFileWriter, wsdlOperation, requestContent);
						} else {
							request = createRequest(wsdlInterface, logFileWriter, wsdlOperation);
						}

						final WsdlResponse response = submitRequest(request, logFileWriter);
						processResponse(response, statistic, service, serviceName, logFileWriter);

						final String actualResponseContent = response.getContentAsString();
						final String expectedResponseContent = operationTest.getResponseContent();

						if (hasText(expectedResponseContent)) {

							final XmlObject expectedXml = XmlObject.Factory.parse(expectedResponseContent);
							final XmlObject actualXml = XmlObject.Factory.parse(actualResponseContent);

							if (expectedXml.valueEquals(actualXml)) {

								logger.debug("Actual and predefined response are matching");
								logFileWriter.writeToLogFile("Actual and predefined response are matching");

							} else {

								statistic.notMatchingResponseOperations.add(operation.getName());

								logger.debug("Actual and expected response are not the same!");
								logFileWriter.writeToLogFile("Actual and expected response are not the same!");

								logger.debug("=== Actual response Content for Operation \"" + operation.getName()
										+ "\" ===");
								logFileWriter.writeToLogFile("=== Actual response Content for Operation \""
										+ operation.getName() + "\" ===");

								logger.debug(actualResponseContent);
								logFileWriter.writeToLogFile(actualResponseContent);

							}
						}

						// TODO if request is a predefined one and not valid => State.CRITICAL, may throw an exeception
						// TODO test with predefined response only

					} catch (final InterruptedException e) {
						logException(e, logFileWriter, operation.getName(), statistic);
					} catch (final SubmitException e) {
						logException(e, logFileWriter, operation.getName(), statistic);
					} catch (final XmlException e) {
						logException(e, logFileWriter, operation.getName(), statistic);
					} catch (final RuntimeException e) {
						logException(e, logFileWriter, operation.getName(), statistic);
					}

				} else {

					final String operationName = operationTest.getOperationName();
					final String message = "predefined operation " + operationName + " not found!";
					logger.warn(message);
					logFileWriter.writeToLogFile(message);
					statistic.predefinedNotFoundOperations.add(operationTest.getOperationName());
				}
			}
		}
	}

	private void monitorAvailableOperations(final LogFileWriter logFileWriter, final Service service,
			final String serviceName, final WsdlInterface wsdlInterface, final Statistic statistic) {

		logFileWriter.writeToLogFile("==== Testing all available operations ====");

		for (final Operation operation : wsdlInterface.getOperationList()) {

			if (operation instanceof WsdlOperation) {

				statistic.operationsTested++;

				try {

					final WsdlOperation wsdlOperation = (WsdlOperation) operation;
					final WsdlRequest request = createRequest(wsdlInterface, logFileWriter, wsdlOperation);
					final WsdlResponse response = submitRequest(request, logFileWriter);
					processResponse(response, statistic, service, serviceName, logFileWriter);

				} catch (final RuntimeException e) {
					logException(e, logFileWriter, operation.getName(), statistic);
				} catch (final InterruptedException e) {
					logException(e, logFileWriter, operation.getName(), statistic);
				} catch (final SubmitException e) {
					logException(e, logFileWriter, operation.getName(), statistic);
				} catch (final XmlException e) {
					logException(e, logFileWriter, operation.getName(), statistic);
				}

			} else {
				logger.warn("operation " + operation.getName() + " is not a WSDL operation!");
			}

		}
	}

	private void processResponse(final WsdlResponse response, final Statistic statistic, final Service service,
			final String serviceName, final LogFileWriter logFileWriter) throws XmlException {

		ComponentHelper.processResponse(response, serviceName, service, logFileWriter);

		// response validation
		final WsdlOperation operation = response.getRequest().getOperation();
		final AssertionError[] responseAssertionErrors = WsdlValidationUtils.validateResponse(response);
		LoggingHelper.logResponseValidation(responseAssertionErrors, logFileWriter);

		if (responseAssertionErrors != null && responseAssertionErrors.length > 0) {
			statistic.invalidResponseOperations.add(operation.getName());
		} else if (WsdlValidationUtils.isSoapFault(response.getContentAsString(), operation)) {
			// SOAP fault test
			statistic.soapFaultOperations.add(operation.getName());
			logFileWriter.writeToLogFile("The response is a SOAP fault!");
		}
	}

	private ServiceStatus buildServiceStatus(final Statistic statistic, final LogFileWriter logFileWriter) {

		// assemble statistic message
		final StringBuffer message = new StringBuffer("Testing Result: ");

		final int problems = statistic.exceptionalOperations.size() + statistic.predefinedNotFoundOperations.size()
				+ statistic.invalidResponseOperations.size() + statistic.notMatchingResponseOperations.size();

		final int operationsTotal = statistic.operationsTested + statistic.predefinedOperationsTested;
		final int successful = operationsTotal - problems;

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

		final String logMessage = message.toString();
		logStatistic(statistic, logFileWriter, logMessage);

		message.append(LoggingHelper.getLogFileText(logFileWriter, logFilesUrl));
		final String statusMessage = message.toString();

		final State state = statistic.getServiceState();
		return new ServiceStatus(statistic.serviceName, statistic.serviceUrl, state, 0, statusMessage);
	}

	private void logStatistic(final Statistic statistic, final LogFileWriter logFileWriter, final String logMessage) {

		logFileWriter.writeToLogFile("==== Testing statistic ====");
		logger.debug("==== Testing statistic ====");

		logFileWriter.writeToLogFile(logMessage);
		logger.debug(logMessage);

		if (!isEmpty(statistic.exceptionalOperations)) {
			logFileWriter.writeToLogFile("=== exceptional operations ===");
			logger.debug("=== exceptional operations ===");
			for (final String operationName : statistic.exceptionalOperations) {
				logFileWriter.writeToLogFile(operationName);
				logger.debug(operationName);
			}
		}

		if (!isEmpty(statistic.invalidResponseOperations)) {
			logFileWriter.writeToLogFile("=== operations with invalid responses ===");
			logger.debug("=== operations with invalid responses ===");
			for (final String operationName : statistic.invalidResponseOperations) {
				logFileWriter.writeToLogFile(operationName);
				logger.debug(operationName);
			}
		}

		if (!isEmpty(statistic.predefinedNotFoundOperations)) {
			logFileWriter.writeToLogFile("=== predefined operations not found ===");
			logger.debug("=== predefined operations not found ===");
			for (final String operationName : statistic.predefinedNotFoundOperations) {
				logFileWriter.writeToLogFile(operationName);
				logger.debug(operationName);
			}
		}

		if (!isEmpty(statistic.soapFaultOperations)) {
			logFileWriter.writeToLogFile("=== operations with SOAP faults ===");
			logger.debug("=== operations with SOAP faults ===");
			for (final String operationName : statistic.soapFaultOperations) {
				logFileWriter.writeToLogFile(operationName);
				logger.debug(operationName);
			}
		}

		if (!isEmpty(statistic.notMatchingResponseOperations)) {
			logFileWriter.writeToLogFile("=== operations with reponse not matching with predefinition ===");
			logger.debug("=== operations with reponse not matching with predefinition ===");
			for (final String operationName : statistic.notMatchingResponseOperations) {
				logFileWriter.writeToLogFile(operationName);
				logger.debug(operationName);
			}
		}
	}

	private void logException(final Exception e, final LogFileWriter logFileWriter, final String operationName,
			final Statistic statistic) {

		statistic.exceptionalOperations.add(operationName);

		logFileWriter.writeToLogFile("An error occured: " + e.getMessage());
		logFileWriter.writeStacktracetoLogFile(e);
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

	private final static class Statistic {

		private final String serviceName;
		private final URL serviceUrl;

		private int operationsTested = 0;
		private int predefinedOperationsTested = 0;

		private final List<String> soapFaultOperations = new ArrayList<String>();
		private final List<String> invalidResponseOperations = new ArrayList<String>();
		private final List<String> exceptionalOperations = new ArrayList<String>();
		private final List<String> predefinedNotFoundOperations = new ArrayList<String>();
		private final List<String> notMatchingResponseOperations = new ArrayList<String>();

		public Statistic(final String serviceName, final URL serviceUrl) {
			this.serviceName = serviceName;
			this.serviceUrl = serviceUrl;
		}

		public State getServiceState() {

			State state = State.OK;
			if (!isEmpty(soapFaultOperations)) {
				state = State.WARNING; // TODO a soap fault must not be an error, it could be an expected result
			}
			if (!isEmpty(exceptionalOperations)) {
				state = State.CRITICAL;
			}
			if (!isEmpty(predefinedNotFoundOperations)) {
				state = State.CRITICAL;
			}
			if (!isEmpty(invalidResponseOperations)) {
				state = State.CRITICAL;
			}
			if (!isEmpty(notMatchingResponseOperations)) {
				state = State.CRITICAL;
			}
			return state;
		}
	}
}
