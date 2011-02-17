package eu.heliovo.monitoring.listener;

import java.util.*;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.failuredetector.FailureDetector;
import eu.heliovo.monitoring.model.Host;
import eu.heliovo.monitoring.test.util.TestServices;

public class ServiceToHostAdapterTest extends Assert {

	private final List<Host> updatedHosts = new ArrayList<Host>();

	@Test
	public void testUpdateServices() {

		List<HostUpdateListener> listeners = Arrays.asList(new HostUpdateListener[] { initMockFailureDetector() });
		ServiceToHostAdapter adapter = new ServiceToHostAdapter(listeners);
		adapter.updateServices(TestServices.LIST);

		List<Host> hostsFromServices = new ArrayList<Host>(adapter.getHostsFromServices(TestServices.LIST));
		for (int i = 0; i < hostsFromServices.size(); i++) {
			assertEquals(hostsFromServices.get(i).getName(), updatedHosts.get(i).getName());
		}
	}
	
	private FailureDetector initMockFailureDetector(){
		
		return new FailureDetector() {

			private boolean alive = false;

			@Override
			public void updateHosts(Set<Host> hosts) {
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
	}
}