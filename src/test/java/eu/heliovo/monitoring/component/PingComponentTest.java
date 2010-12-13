package eu.heliovo.monitoring.component;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.failuredetector.FailureDetectorTestUtils;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.test.util.TestServices;

public class PingComponentTest extends Assert {

	@Test
	public void testPingComponent() throws Exception {

		final PingComponent pingComponent = new PingComponent(FailureDetectorTestUtils.getServiceFailureDetector());
		pingComponent.setServices(TestServices.LIST);
		pingComponent.updateStatus();

		final List<ServiceStatusDetails> serviceStatusDetails = pingComponent.getServicesStatus();
		assertNotNull(serviceStatusDetails);
		assertTrue(serviceStatusDetails.size() == TestServices.LIST.size());

		boolean testedFakeService = false;
		for (final ServiceStatusDetails actualServiceStatusDetails : serviceStatusDetails) {
			if (actualServiceStatusDetails.getName().equals("FakeOfflineService" + PingComponent.SERVICE_NAME_SUFFIX)) {
				testedFakeService = true;
				assertTrue(actualServiceStatusDetails.getStatus().equals(ServiceStatus.CRITICAL));
			}
		}
		assertTrue(testedFakeService);
	}
}
