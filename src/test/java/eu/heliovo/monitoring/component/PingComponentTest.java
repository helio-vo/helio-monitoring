package eu.heliovo.monitoring.component;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.ServiceStatusDetails;
import eu.heliovo.monitoring.model.Status;
import eu.heliovo.monitoring.statics.Services;

public class PingComponentTest extends Assert {

	@Test
	public void testPingComponent() throws Exception {

		final PingComponent pingComponent = new PingComponent();
		pingComponent.setServices(Services.LIST);
		pingComponent.refreshCache();

		final List<ServiceStatusDetails> serviceStatusDetails = pingComponent.getStatus();
		assertNotNull(serviceStatusDetails);
		assertTrue(serviceStatusDetails.size() == Services.LIST.size());

		boolean testedFakeService = false;
		for (final ServiceStatusDetails actualServiceStatusDetails : serviceStatusDetails) {
			if (actualServiceStatusDetails.getId().equals("FakeOfflineService" + pingComponent.getServiceNameSuffix())) {
				testedFakeService = true;
				assertTrue(actualServiceStatusDetails.getStatus().equals(Status.CRITICAL));
			}
		}
		assertTrue(testedFakeService);
	}
}
