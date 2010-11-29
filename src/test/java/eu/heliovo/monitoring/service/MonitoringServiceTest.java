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
import eu.heliovo.monitoring.daemon.RemotingMonitoringDaemon;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.serviceloader.ServiceLoader;
import eu.heliovo.monitoring.serviceloader.StaticServiceLoader;

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
		methodCallComponent = new MethodCallComponent(componentHelper, "mainlog", logFilesUrl);
		testingComponent = new TestingComponent(componentHelper, "mainlog", logFilesUrl);

		ServiceLoader serviceLoader = new StaticServiceLoader();

		RemotingMonitoringDaemon daemon = new RemotingMonitoringDaemon() {
			@Override
			public void writeServiceStatusToNagios(List<ServiceStatus> serviceStatus) {
			}
		};

		monitoringService = new MonitoringServiceImpl(pingComponent, methodCallComponent, testingComponent,
				serviceLoader, daemon, daemon, null);
	}

	@Test
	public void testService() throws Exception {

		// these lines are automatically called by spring
		monitoringService.afterPropertiesSet();
		monitoringService.updatePingStatusAndWriteToNagios();
		monitoringService.updateMethodCallStatusAndWriteToNagios();
		monitoringService.updateTestingStatusAndWriteToNagios();

		// pingComponent.refreshCache();
		// methodCallComponent.refreshCache();
		// testingComponent.refreshCache();

		List<ServiceStatus> result = monitoringService.getPingStatus();
		assertFalse(result.isEmpty());
		for (final ServiceStatus serviceStatus : result) {
			System.out.println("service: " + serviceStatus.getId() + " status: " + serviceStatus.getState().toString()
					+ " response time: " + serviceStatus.getResponseTime() + " ms");
		}

		result = monitoringService.getMethodCallStatus();
		assertFalse(result.isEmpty());
		for (final ServiceStatus serviceStatus : result) {
			System.out.println("service: " + serviceStatus.getId() + " status: " + serviceStatus.getState().toString()
					+ " response time: " + serviceStatus.getResponseTime() + " ms");
		}

		result = monitoringService.getTestingStatus();
		assertFalse(result.isEmpty());
		for (final ServiceStatus serviceStatus : result) {
			System.out.println("service: " + serviceStatus.getId() + " status: " + serviceStatus.getState().toString()
					+ " response time: " + serviceStatus.getResponseTime() + " ms");
		}
		// TODO do it as well for method call and testing
	}
}
