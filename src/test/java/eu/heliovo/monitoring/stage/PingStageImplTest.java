package eu.heliovo.monitoring.stage;

import java.util.*;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.failuredetector.FailureDetector;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.test.util.TestServices;
import eu.heliovo.monitoring.util.ServiceHostUtils;

public class PingStageImplTest extends Assert {

	private final List<Host> updatedHosts = new ArrayList<Host>();
	private final FailureDetector mockFailureDetector = initMockFailureDetector();

	@Test
	public void testGetStatus() throws Exception {

		Set<Host> hosts = ServiceHostUtils.getHostsFromServices(TestServices.LIST);

		mockFailureDetector.updateHosts(hosts);

		PingStage pingStage = new PingStageImpl(mockFailureDetector);

		List<StatusDetails<Host>> status = pingStage.getStatus(hosts);
		assertEquals(hosts.size(), status.size());

		for (StatusDetails<Host> serviceStatusDetails : status) {

			assertTrue(hosts.contains(serviceStatusDetails.getMonitoredEntity()));

			Host host = serviceStatusDetails.getMonitoredEntity();
			String originalHostName = host.getName();
			assertEquals(originalHostName, serviceStatusDetails.getName());

			System.out.println(serviceStatusDetails);

			if (serviceStatusDetails.getStatus().equals(Status.CRITICAL)) {
				assertEquals(Long.MAX_VALUE, serviceStatusDetails.getResponseTimeInMillis());
			} else if (serviceStatusDetails.getStatus().equals(Status.OK)) {
				assertTrue(serviceStatusDetails.getResponseTimeInMillis() >= 0);
			} else {
				fail(); // only OK and CRITICAL are allowed
			}
		}
	}

	private FailureDetector initMockFailureDetector() {
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