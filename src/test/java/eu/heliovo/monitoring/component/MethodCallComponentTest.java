package eu.heliovo.monitoring.component;

import static eu.heliovo.monitoring.logging.LoggingUtils.getLoggingFactory;
import static eu.heliovo.monitoring.test.util.TestUtils.getComponentHelper;
import static eu.heliovo.monitoring.test.util.TestUtils.logFilesUrl;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.test.util.TestServices;
public class MethodCallComponentTest extends Assert {

	@Test
	public void testMethodCallComponent() throws Exception {

		// TODO test individual methods of MethodCallComponent

		MethodCallComponent component = new MethodCallComponent(getComponentHelper(), getLoggingFactory(), logFilesUrl);
		component.setServices(TestServices.LIST);
		component.refreshCache();

		final List<ServiceStatusDetails> serviceStatus = component.getStatus();

		assertNotNull(serviceStatus);
		assertTrue(serviceStatus.size() == TestServices.LIST.size());

		boolean testedFakeService = false;
		boolean testedNoWsdlService = false;
		for (final ServiceStatusDetails actualServiceStatusDetails : serviceStatus) {
			if (actualServiceStatusDetails.getId().equals("FakeOfflineService" + component.getServiceNameSuffix())) {
				testedFakeService = true;
				assertTrue(actualServiceStatusDetails.getStatus().equals(ServiceStatus.CRITICAL));
			}
			if (actualServiceStatusDetails.getId().equals("NoWsdlOfflineService" + component.getServiceNameSuffix())) {
				testedNoWsdlService = true;
				assertTrue(actualServiceStatusDetails.getStatus().equals(ServiceStatus.CRITICAL));
			}
		}
		assertTrue(testedFakeService);
		assertTrue(testedNoWsdlService);

		System.out.println("=== Services to be tested ===");
		for (final Service service : TestServices.LIST) {
			System.out.println(service.getName() + " " + service.getUrl());
		}

		System.out.println("=== testing results");
		for (final ServiceStatusDetails serviceStatusDetails : component.getStatus()) {
			System.out.println(serviceStatusDetails.toString());
		}
	}
}
