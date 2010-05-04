package eu.heliovo.monitoring.component;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.State;
import eu.heliovo.monitoring.statics.Services;

public class MethodCallComponentTest extends Assert {

	@Test
	public void testMethodCallComponent() throws Exception {

		final MethodCallComponent component = new MethodCallComponent("mainlog",
				"http://localhost:8080/helio-monitoring/logs");
		component.setServices(Services.list);
		component.refreshCache();

		final List<ServiceStatus> serviceStatus = component.getStatus();

		assertNotNull(serviceStatus);
		assertTrue(serviceStatus.size() == Services.list.size());

		boolean testedFakeService = false;
		boolean testedNoWsdlService = false;
		for (final ServiceStatus actualServiceStatus : serviceStatus) {
			if (actualServiceStatus.getId().equals("FakeOfflineService" + component.SERVICE_NAME_SUFFIX)) {
				testedFakeService = true;
				assertTrue(actualServiceStatus.getState().equals(State.CRITICAL));
			}
			if (actualServiceStatus.getId().equals("NoWsdlOfflineService" + component.SERVICE_NAME_SUFFIX)) {
				testedNoWsdlService = true;
				assertTrue(actualServiceStatus.getState().equals(State.CRITICAL));
			}
		}
		assertTrue(testedFakeService);
		assertTrue(testedNoWsdlService);

		System.out.println("=== Services to be tested ===");
		for (final Service service : Services.list) {
			System.out.println(service.getName() + " " + service.getUrl());
		}

		System.out.println("=== testing results");
		for (final ServiceStatus status : component.getStatus()) {
			System.out.println(status.toString());
		}
	}
}
