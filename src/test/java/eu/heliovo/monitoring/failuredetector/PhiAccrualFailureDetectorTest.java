package eu.heliovo.monitoring.failuredetector;

import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.test.util.*;

public class PhiAccrualFailureDetectorTest extends Assert {

	private final ScheduledExecutorService executor = TestUtils.getScheduledExecutor();
	private final PhiAccrualFailureDetector failureDetector = new PhiAccrualFailureDetector(TestUtils.getExecutor());

	public PhiAccrualFailureDetectorTest() throws Exception {
	}

	@Test
	public void testFailureDetector() throws Exception {

		List<Host> monitoredHosts = getHostsToBeMonitored();
		failureDetector.updateHosts(monitoredHosts);

		startRegularlyFailureDetection();
		waitSomeTimeToGetResults();
		outputResults(monitoredHosts);

		testHostNotMonitored();
		
		executor.shutdown();
		executor.awaitTermination(60, TimeUnit.SECONDS);
	}

	private List<Host> getHostsToBeMonitored() {
		ServiceToHostAdapter serviceToHostAdapter = new ServiceToHostAdapter(null);
		return serviceToHostAdapter.getHostsFromServices(TestServices.LIST);
	}

	private void startRegularlyFailureDetection() {
		Runnable detectFailureTask = new Runnable() {
			@Override
			public void run() {
				failureDetector.detect();
			}
		};
		executor.scheduleAtFixedRate(detectFailureTask, 0, 1, TimeUnit.SECONDS);
	}

	private void waitSomeTimeToGetResults() throws InterruptedException {
		Thread.sleep(4500);
	}

	private void outputResults(List<Host> hosts) {
		for (Host host : hosts) {
			System.out.print("host: " + host.getName());
			System.out.print(" isAlive: " + failureDetector.isAlive(host));
			System.out.println(" responseTimeInMillis: " + failureDetector.getResponseTimeInMillis(host));
		}
	}

	private void testHostNotMonitored() throws Exception {

		Host noMonitoredHost = ModelFactory.newHost(new URL("http://www.google.com"), new ArrayList<Service>());

		boolean exceptionThrown = false;
		try {
			failureDetector.isAlive(noMonitoredHost);
		} catch (IllegalArgumentException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}
}