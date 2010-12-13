package eu.heliovo.monitoring.service;

import static eu.heliovo.monitoring.test.util.TestUtils.getComponentHelper;
import static eu.heliovo.monitoring.test.util.TestUtils.logFilesUrl;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.exporter.ServiceStatusDetailsExporter;
import eu.heliovo.monitoring.failuredetector.*;
import eu.heliovo.monitoring.logging.LoggingTestUtils;
import eu.heliovo.monitoring.model.ServiceStatusDetails;
import eu.heliovo.monitoring.serviceloader.*;
import eu.heliovo.monitoring.stage.*;

public class MonitoringServiceTest extends Assert {

	private final PingStage pingComponent;
	private final MethodCallStage methodCallComponent;
	private final TestingStage testingComponent;

	private final MonitoringServiceImpl monitoringService;

	public MonitoringServiceTest() throws Exception {

		StageHelper componentHelper = getComponentHelper();

		ServiceFailureDetector failureDetector = FailureDetectorTestUtils.getServiceFailureDetector();
		pingComponent = new PingStage(failureDetector);
		methodCallComponent = new MethodCallStage(componentHelper, LoggingTestUtils.getLoggingFactory(), logFilesUrl);
		testingComponent = new TestingStage(componentHelper, LoggingTestUtils.getLoggingFactory(), logFilesUrl);

		ServiceLoader serviceLoader = new StaticServiceLoader();

		ServiceStatusDetailsExporter exporter = new ServiceStatusDetailsExporter() {
			@Override
			public void exportServiceStatusDetails(List<ServiceStatusDetails> serviceStatus) {
			}
		};

		monitoringService = new MonitoringServiceImpl(pingComponent, methodCallComponent, testingComponent,
				serviceLoader, exporter);
	}

	@Test
	public void testService() throws Exception {

		// these lines are automatically called by spring
		monitoringService.updatePingStatusAndExport();
		monitoringService.updateMethodCallStatusAndExport();
		monitoringService.updateTestingStatusAndExport();

		List<ServiceStatusDetails> result = monitoringService.getPingStatus();
		assertFalse(result.isEmpty());
		for (final ServiceStatusDetails serviceStatusDetails : result) {
			System.out.println("service: " + serviceStatusDetails.getName() + " status: "
					+ serviceStatusDetails.getStatus().toString() + " response time: "
					+ serviceStatusDetails.getResponseTimeInMillis() + " ms");
		}

		result = monitoringService.getMethodCallStatus();
		assertFalse(result.isEmpty());
		for (final ServiceStatusDetails serviceStatusDetails : result) {
			System.out.println("service: " + serviceStatusDetails.getName() + " status: "
					+ serviceStatusDetails.getStatus().toString() + " response time: "
					+ serviceStatusDetails.getResponseTimeInMillis() + " ms");
		}

		result = monitoringService.getTestingStatus();
		assertFalse(result.isEmpty());
		for (final ServiceStatusDetails serviceStatusDetails : result) {
			System.out.println("service: " + serviceStatusDetails.getName() + " status: "
					+ serviceStatusDetails.getStatus().toString() + " response time: "
					+ serviceStatusDetails.getResponseTimeInMillis() + " ms");
		}
	}
}
