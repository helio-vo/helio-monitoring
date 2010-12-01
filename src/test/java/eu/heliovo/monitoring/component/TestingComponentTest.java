package eu.heliovo.monitoring.component;

import static eu.heliovo.monitoring.test.util.TestUtils.getComponentHelper;
import static eu.heliovo.monitoring.test.util.TestUtils.logFilesUrl;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatusDetails;
import eu.heliovo.monitoring.model.Status;
import eu.heliovo.monitoring.statics.Services;

public class TestingComponentTest {

	@Test
	public final void testRefreshCache() throws Exception {

		final TestingComponent component = new TestingComponent(getComponentHelper(), "mainlog", logFilesUrl);
		component.setServices(Services.LIST);
		component.refreshCache();

		final List<ServiceStatusDetails> serviceStatus = component.getStatus();

		assertNotNull(serviceStatus);
		assertTrue(serviceStatus.size() == Services.LIST.size());

		boolean testedFakeService = false;
		boolean testedNoWsdlService = false;
		for (final ServiceStatusDetails actualServiceStatusDetails : serviceStatus) {
			if (actualServiceStatusDetails.getId().equals("FakeOfflineService" + component.getServiceNameSuffix())) {
				testedFakeService = true;
				assertTrue(actualServiceStatusDetails.getStatus().equals(Status.CRITICAL));
			}
			if (actualServiceStatusDetails.getId().equals("NoWsdlOfflineService" + component.getServiceNameSuffix())) {
				testedNoWsdlService = true;
				assertTrue(actualServiceStatusDetails.getStatus().equals(Status.CRITICAL));
			}
		}
		assertTrue(testedFakeService);
		assertTrue(testedNoWsdlService);

		System.out.println("=== Services to be tested ===");
		for (final Service service : Services.LIST) {
			System.out.println(service.getName() + " " + service.getUrl());
		}

		System.out.println("=== testing results");
		for (final ServiceStatusDetails serviceStatusDetails : component.getStatus()) {
			System.out.println(serviceStatusDetails.toString());
		}

	}

}
