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
		pingComponent.setServices(Services.list);
		pingComponent.refreshCache();

		final List<ServiceStatus> serviceStatus = pingComponent.getStatus();
		assertNotNull(serviceStatus);
		assertTrue(serviceStatus.size() == Services.list.size());

		boolean testedFakeService = false;
		for (final ServiceStatus actualServiceStatus : serviceStatus) {
			if (actualServiceStatus.getId().equals("FakeOfflineService" + pingComponent.SERVICE_NAME_SUFFIX)) {
				testedFakeService = true;
				assertTrue(actualServiceStatus.getState().equals(State.DOWN));
			}
		}
		assertTrue(testedFakeService);
	}
}
