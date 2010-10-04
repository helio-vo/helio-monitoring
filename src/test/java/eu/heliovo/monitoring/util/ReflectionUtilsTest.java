package eu.heliovo.monitoring.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import eu.heliovo.monitoring.model.OperationTest;
import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceFactory;
import eu.heliovo.monitoring.model.TestingService;

public class ReflectionUtilsTest {

	@Test
	public void testImplementsInterface() throws Exception {

		final Service service = ServiceFactory.newService("Service", new URL("http://www.helio-vo.eu/"));
		assertFalse(ReflectionUtils.implementsInterface(service, TestingService.class));

		final List<OperationTest> requests = Collections.emptyList();
		final URL serviceUrl = new URL("http://www.helio-vo.eu/");
		final Service serviceWithRequests = ServiceFactory.newService("Service", serviceUrl, requests);
		assertTrue(ReflectionUtils.implementsInterface(serviceWithRequests, TestingService.class));
	}
}
