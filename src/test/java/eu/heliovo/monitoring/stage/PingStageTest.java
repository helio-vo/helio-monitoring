package eu.heliovo.monitoring.stage;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.failuredetector.FailureDetectorTestUtils;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.stage.PingStage;
import eu.heliovo.monitoring.test.util.TestServices;

public class PingStageTest extends Assert {

	@Test
	public void testPingComponent() throws Exception {

		final PingStage pingComponent = new PingStage(FailureDetectorTestUtils.getServiceFailureDetector());
		pingComponent.setServices(TestServices.LIST);
		pingComponent.updateStatus();

		final List<ServiceStatusDetails> serviceStatusDetails = pingComponent.getServicesStatus();
		assertNotNull(serviceStatusDetails);
		assertTrue(serviceStatusDetails.size() == TestServices.LIST.size());

		boolean testedFakeService = false;
		for (final ServiceStatusDetails actualServiceStatusDetails : serviceStatusDetails) {
			if (actualServiceStatusDetails.getName().equals("FakeOfflineService" + PingStage.SERVICE_NAME_SUFFIX)) {
				testedFakeService = true;
				assertTrue(actualServiceStatusDetails.getStatus().equals(ServiceStatus.CRITICAL));
			}
		}
		assertTrue(testedFakeService);
	}
}
