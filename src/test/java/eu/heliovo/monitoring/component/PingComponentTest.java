package eu.heliovo.monitoring.component;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.component.PingComponent;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.State;

public class PingComponentTest extends Assert {

	@Test
	public void testPingComponent() throws Exception {

		final Map<String, URL> services = new HashMap<String, URL>();
		services.put("HEC local", new URL("http://localhost:8080/core/HECService?wsdl"));
		services.put("FrontendFacade local", new URL("http://localhost:8080/core/FrontendFacadeService?wsdl"));
		services.put("HEC", new URL("http://helio.i4ds.technik.fhnw.ch:8080/core/HECService?wsdl"));
		services.put("FrontendFacade",
				new URL("http://helio.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl"));
		services.put("FakeOfflineService", new URL("http://123.43.121.11/"));

		final PingComponent pingComponent = new PingComponent();
		pingComponent.setServices(services);
		pingComponent.refreshCache();

		final List<ServiceStatus> serviceStatus = pingComponent.getStatus();
		assertNotNull(serviceStatus);
		assertTrue(serviceStatus.size() == services.size());

		boolean testedFakeService = false;
		for (final ServiceStatus actualServiceStatus : serviceStatus) {
			if (actualServiceStatus.getId().equals("FakeOfflineService")) {
				testedFakeService = true;
				assertTrue(actualServiceStatus.getState().equals(State.DOWN));
			}
		}
		assertTrue(testedFakeService);
	}
}
