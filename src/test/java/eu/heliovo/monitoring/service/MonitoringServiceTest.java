package eu.heliovo.monitoring.service;

import static eu.heliovo.monitoring.test.util.TestUtils.getComponentHelper;
import static eu.heliovo.monitoring.test.util.TestUtils.logFilesUrl;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.component.ComponentHelper;
import eu.heliovo.monitoring.component.MethodCallComponent;
import eu.heliovo.monitoring.component.PingComponent;
import eu.heliovo.monitoring.component.TestingComponent;
import eu.heliovo.monitoring.model.ServiceStatus;

/**
 * Tests the MonitoringService.
 * 
 * @author Kevin Seidler
 * 
 */
public class MonitoringServiceTest extends Assert {

	private final PingComponent pingComponent = new PingComponent();
	private final MethodCallComponent methodCallComponent;
	private final TestingComponent testingComponent;

	private final MonitoringServiceImpl monitoringService;

	public MonitoringServiceTest() throws Exception {

		ComponentHelper componentHelper = getComponentHelper();

		methodCallComponent = new MethodCallComponent(componentHelper, "mainlog", logFilesUrl);
		testingComponent = new TestingComponent(componentHelper, "mainlog", logFilesUrl);

		monitoringService = new MonitoringServiceImpl(pingComponent, methodCallComponent, testingComponent, null, null,
				null, null);
	}

	@Test
	public void testService() throws Exception {

		monitoringService.afterPropertiesSet(); // called automatically by spring
		pingComponent.refreshCache(); // also done automatically in runtime
		testingComponent.refreshCache(); // also done automatically in runtime

		// TODO evenutally call MethodCallComponent here

		final List<ServiceStatus> result = monitoringService.getPingStatus();
		assertFalse(result.isEmpty());
		for (final ServiceStatus serviceStatus : result) {
			System.out.println("service: " + serviceStatus.getId() + " status: " + serviceStatus.getState().toString()
					+ " response time: " + serviceStatus.getResponseTime() + " ms");
		}

		// TODO do it as well for method call and testing
	}
}
