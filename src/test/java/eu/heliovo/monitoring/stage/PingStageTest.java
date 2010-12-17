package eu.heliovo.monitoring.stage;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.failuredetector.FailureDetectorTestUtils;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.test.util.TestServices;

public class PingStageTest extends Assert {

	@Test
	public void testPingStage() throws Exception {

		// FIXME returns status CRITICAL for every service

		final PingStage pingStage = new PingStage(FailureDetectorTestUtils.getServiceFailureDetector());
		pingStage.updateStatus();

		final List<ServiceStatusDetails> serviceStatusDetails = pingStage.getServicesStatus();
		assertNotNull(serviceStatusDetails);
		assertTrue(serviceStatusDetails.size() == TestServices.LIST.size());

		boolean testedFakeService = false;
		for (final ServiceStatusDetails actualServiceStatusDetails : serviceStatusDetails) {
			System.out.println(actualServiceStatusDetails);
			if (actualServiceStatusDetails.getName().equals("FakeOfflineService" + PingStage.SERVICE_NAME_SUFFIX)) {
				testedFakeService = true;
				assertTrue(actualServiceStatusDetails.getStatus().equals(ServiceStatus.CRITICAL));
			}
		}
		assertTrue(testedFakeService);
	}
}