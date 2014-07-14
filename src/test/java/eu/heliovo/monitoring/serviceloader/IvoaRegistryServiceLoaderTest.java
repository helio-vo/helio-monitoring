package eu.heliovo.monitoring.serviceloader;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.test.util.TestUtils;

public class IvoaRegistryServiceLoaderTest extends Assert {

	private static final String registryUrl = "http://helio.mssl.ucl.ac.uk/helio_registry/services/RegistryQueryv1_0";

	private final ExecutorService executor;

	public IvoaRegistryServiceLoaderTest() throws Exception {
		executor = TestUtils.getExecutor();
	}

	@Test
	public void testLoadServices() throws Exception {

		ServiceLoader serviceloader = new IvoaRegistryServiceLoader(registryUrl, executor);
		Set<Service> services = serviceloader.loadServices();
		assertTrue(services.size() > 0); // at least one service should be registered.
		for (Service service : services) {
			System.out.println(service.getIdentifier() + " - " + service.getName() + " - " + service.getUrl());
		}
		
		// at this url there is no registry, getting 404 and the result is an empty list
		serviceloader = new IvoaRegistryServiceLoader("http://imaginary.com/Registry", executor);
		services = serviceloader.loadServices();
		assertTrue(services.isEmpty());

		// host is not existing, connection timeout occurs and results is an empty list
		serviceloader = new IvoaRegistryServiceLoader("http://123.43.121.11/Registry", executor);
		services = serviceloader.loadServices();
		assertTrue(services.isEmpty());
	}
}