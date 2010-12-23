package eu.heliovo.monitoring.util;

import java.util.*;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.test.util.TestServices;

public class ServiceHostUtilsTest extends Assert {

	@Test
	public void testGetHostsFromServices() {

		Set<Host> hosts = ServiceHostUtils.getHostsFromServices(TestServices.LIST);

		Set<Service> allServicesOfAllHosts = new HashSet<Service>();
		for (Host host : hosts) {
			System.out.println("host: " + host.getName() + " url: " + host.getUrl());
			for (Service service : host.getServices()) {
				assertEquals(host.getName(), service.getUrl().getHost());
				allServicesOfAllHosts.add(service);
				System.out.println("service: " + service.getName() + " url: " + service.getUrl());
			}
			System.out.println();
		}

		assertEquals(TestServices.LIST.size(), allServicesOfAllHosts.size());
	}
}