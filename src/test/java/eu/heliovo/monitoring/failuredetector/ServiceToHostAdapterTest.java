package eu.heliovo.monitoring.failuredetector;

import java.util.*;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.listener.HostUpdateListener;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.test.util.TestServices;

public class ServiceToHostAdapterTest extends Assert {

	private final List<Host> updatedHosts = new ArrayList<Host>();

	private final FailureDetector mockFailureDetector = new FailureDetector() {

		private boolean alive = false;

		@Override
		public void updateHosts(List<Host> hosts) {
			updatedHosts.addAll(hosts);
		}

		@Override
		public boolean isAlive(Host host) {
			alive = !alive; // to test true and false cases
			return alive;
		}

		@Override
		public long getResponseTimeInMillis(Host host) {
			return 10;
		}
	};

	@Test
	public void testGetHostsFromServices() throws Exception {

		List<HostUpdateListener> listeners = Collections.emptyList();
		ServiceToHostAdapter adapter = new ServiceToHostAdapter(null, listeners);
		List<Host> hosts = adapter.getHostsFromServices(TestServices.LIST);

		List<Service> allServicesOfAllHosts = new ArrayList<Service>();

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

	@Test
	public void testUpdateServices() {

		List<HostUpdateListener> listeners = Arrays.asList(new HostUpdateListener[] { mockFailureDetector });
		ServiceToHostAdapter adapter = new ServiceToHostAdapter(mockFailureDetector, listeners);
		adapter.updateServices(TestServices.LIST);

		List<Host> hostsFromServices = adapter.getHostsFromServices(TestServices.LIST);

		for (int i = 0; i < hostsFromServices.size(); i++) {
			assertEquals(hostsFromServices.get(i).getName(), updatedHosts.get(i).getName());
		}
	}
	
	@Test
	public void testGetServiceStatus() {

		List<HostUpdateListener> listeners = Arrays.asList(new HostUpdateListener[] { mockFailureDetector });
		ServiceToHostAdapter adapter = new ServiceToHostAdapter(mockFailureDetector, listeners);
		adapter.updateServices(TestServices.LIST);
		
		String serviceNameSuffix = "serviceNameSuffix";
		List<ServiceStatusDetails> status = adapter.getServicesStatus("serviceNameSuffix");
		
		assertEquals(TestServices.LIST.size(), status.size());
		
		for (ServiceStatusDetails serviceStatusDetails : status) {

			System.out.println(serviceStatusDetails);

			assertTrue(serviceStatusDetails.getName().endsWith(serviceNameSuffix));

			if (serviceStatusDetails.getStatus().equals(ServiceStatus.CRITICAL)) {

				assertEquals(Long.MAX_VALUE, serviceStatusDetails.getResponseTimeInMillis());

			} else if (serviceStatusDetails.getStatus().equals(ServiceStatus.OK)) {

				assertTrue(serviceStatusDetails.getResponseTimeInMillis() >= 0);

			} else {
				fail(); // only OK and CRITICAL are allowed
			}
		}
		
	}
}