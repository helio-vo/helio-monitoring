package eu.heliovo.monitoring.service;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.component.PingComponent;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.service.MonitoringServiceImpl;

/**
 * Tests the MonitoringService.
 * 
 * @author Kevin Seidler
 * 
 */
public class MonitoringServiceTest extends Assert {

	private final PingComponent pingComponent = new PingComponent();
	private final MonitoringServiceImpl monitoringService = new MonitoringServiceImpl(pingComponent);

	@Test
	public void testService() throws Exception {

		monitoringService.afterPropertiesSet(); // called automatically by
												// spring
		pingComponent.refreshCache(); // also done automatically in runtime

		final List<ServiceStatus> result = monitoringService.getPingStatus();
		assertFalse(result.isEmpty());
		for (final ServiceStatus serviceStatus : result) {
			System.out.println("service: " + serviceStatus.getId() + " status: " + serviceStatus.getState().toString()
					+ " response time: " + serviceStatus.getResponseTime() + " ms");
		}
	}
}
