package eu.heliovo.monitoring.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.*;

import org.junit.Test;

import eu.heliovo.monitoring.model.*;

public class ReflectionUtilsTest {

	@Test
	public void testImplementsInterface() throws Exception {

		final Service service = ModelFactory.newService("", "Service", new URL("http://www.helio-vo.eu/"));
		assertFalse(ReflectionUtils.implementsInterface(service, TestingService.class));

		final List<OperationTest> requests = Collections.emptyList();
		final URL serviceUrl = new URL("http://www.helio-vo.eu/");
		final Service serviceWithRequests = ModelFactory.newService("", "Service", serviceUrl, requests);
		assertTrue(ReflectionUtils.implementsInterface(serviceWithRequests, TestingService.class));
	}
}
