package eu.heliovo.monitoring.service;

import static eu.heliovo.monitoring.test.util.TestUtils.getComponentHelper;
import static eu.heliovo.monitoring.test.util.TestUtils.logFilesUrl;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.component.*;
import eu.heliovo.monitoring.exporter.ServiceStatusDetailsExporter;
import eu.heliovo.monitoring.logging.LoggingUtils;
import eu.heliovo.monitoring.model.ServiceStatusDetails;
import eu.heliovo.monitoring.serviceloader.*;

/**
 * Tests the MonitoringService.
 * 
 * @author Kevin Seidler
 * 
 */
public class MonitoringServiceTest extends Assert {

	private final PingComponent pingComponent;
	private final MethodCallComponent methodCallComponent;
	private final TestingComponent testingComponent;

	private final MonitoringServiceImpl monitoringService;

	public MonitoringServiceTest() throws Exception {

		ComponentHelper componentHelper = getComponentHelper();

		pingComponent = new PingComponent();
		methodCallComponent = new MethodCallComponent(componentHelper, LoggingUtils.getLoggingFactory(), logFilesUrl);
		testingComponent = new TestingComponent(componentHelper, LoggingUtils.getLoggingFactory(), logFilesUrl);

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
			System.out.println("service: " + serviceStatusDetails.getId() + " status: "
					+ serviceStatusDetails.getStatus().toString() + " response time: "
					+ serviceStatusDetails.getResponseTimeInMillis() + " ms");
		}

		result = monitoringService.getMethodCallStatus();
		assertFalse(result.isEmpty());
		for (final ServiceStatusDetails serviceStatusDetails : result) {
			System.out.println("service: " + serviceStatusDetails.getId() + " status: "
					+ serviceStatusDetails.getStatus().toString() + " response time: "
					+ serviceStatusDetails.getResponseTimeInMillis() + " ms");
		}

		result = monitoringService.getTestingStatus();
		assertFalse(result.isEmpty());
		for (final ServiceStatusDetails serviceStatusDetails : result) {
			System.out.println("service: " + serviceStatusDetails.getId() + " status: "
					+ serviceStatusDetails.getStatus().toString() + " response time: "
					+ serviceStatusDetails.getResponseTimeInMillis() + " ms");
		}
	}
}
