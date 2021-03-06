package eu.heliovo.monitoring.serviceloader;

import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.statics.Services;

public class StaticServiceLoaderTest extends Assert {
	
	@Test
	public void testLoadServices() throws Exception {
		
		ServiceLoader serviceLoader = new StaticServiceLoader();
		Set<Service> services = serviceLoader.loadServices();
		assertTrue(services == Services.LIST);
	}
}