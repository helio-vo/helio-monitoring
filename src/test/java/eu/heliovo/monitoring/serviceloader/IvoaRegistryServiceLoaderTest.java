package eu.heliovo.monitoring.serviceloader;

import java.util.List;
import java.util.concurrent.ExecutorService;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.test.util.TestUtils;

public class IvoaRegistryServiceLoaderTest extends Assert {

	private static final String registryUrl = "http://msslxw.mssl.ucl.ac.uk:8080/helio_registry/services/RegistryQueryv1_0";
	
	private final ExecutorService executor;

	public IvoaRegistryServiceLoaderTest() throws Exception {
		executor = TestUtils.getExecutor();
	}
	
	@Test
	public void testRegistryClient() throws Exception {
		
		ServiceLoader serviceloader = new IvoaRegistryServiceLoader(registryUrl, executor);
		List<Service> services = serviceloader.loadServices();
		for (Service service : services) {
			System.out.println(service.getName() + " " + service.getUrl());
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