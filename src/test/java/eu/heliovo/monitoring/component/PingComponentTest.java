package eu.heliovo.monitoring.component;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.State;
import eu.heliovo.monitoring.statics.Services;

public class PingComponentTest extends Assert {

	@Test
	public void testPingComponent() throws Exception {

		final PingComponent pingComponent = new PingComponent();
		pingComponent.setServices(Services.LIST);
		pingComponent.refreshCache();

		final List<ServiceStatus> serviceStatus = pingComponent.getStatus();
		assertNotNull(serviceStatus);
		assertTrue(serviceStatus.size() == Services.LIST.size());

		boolean testedFakeService = false;
		for (final ServiceStatus actualServiceStatus : serviceStatus) {
			if (actualServiceStatus.getId().equals("FakeOfflineService" + pingComponent.getServiceNameSuffix())) {
				testedFakeService = true;
				assertTrue(actualServiceStatus.getState().equals(State.CRITICAL));
			}
		}
		assertTrue(testedFakeService);
	}
}
