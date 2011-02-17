package eu.heliovo.monitoring.failuredetector;

import java.util.Set;

import org.junit.Ignore;

import eu.heliovo.monitoring.model.Host;
import eu.heliovo.monitoring.test.util.*;
import eu.heliovo.monitoring.util.ServiceHostUtils;

@Ignore
public final class FailureDetectorTestUtils {

	private FailureDetectorTestUtils() {
	}

	public static HostStatisticsRecorder getEmptyStatisticsRecorder() {
		return new HostStatisticsRecorder() {
			@Override
			public void updateHosts(Set<Host> newHosts) {
			}

			@Override
			public long getNextEntryId(Host host) {
				return 0;
			}

			@Override
			public void record(Host host, long entryId, long measure) {
			}

			@Override
			public void record(Host host, long entryId, Exception error) {
			}
		};
	}

	public static FailureDetector getFailureDetector() throws Exception {

		PhiAccrualFailureDetector failureDetector;
		failureDetector = new PhiAccrualFailureDetector(TestUtils.getExecutor(), getEmptyStatisticsRecorder());

		failureDetector.updateHosts(ServiceHostUtils.getHostsFromServices(TestServices.LIST));
		detectForResults(failureDetector);

		return failureDetector;
	}

	private static void detectForResults(PhiAccrualFailureDetector failureDetector) throws InterruptedException {
		failureDetector.detect(); // detect at least one time and wait to have results for testing
		Thread.sleep(1500);
		failureDetector.detect();
		Thread.sleep(1500);
	}

	// public static ServiceFailureDetector getServiceFailureDetector() throws Exception {
	//
	// PhiAccrualFailureDetector failureDetector;
	// failureDetector = new PhiAccrualFailureDetector(TestUtils.getExecutor(), getEmptyStatisticsRecorder());
	//
	// ServiceFailureDetector serviceFailureDetector = initServiceFailureDetector(failureDetector);
	// detectForResults(failureDetector);
	//
	// return serviceFailureDetector;
	// }

	// private static ServiceFailureDetector initServiceFailureDetector(PhiAccrualFailureDetector failureDetector) {
	//
	// List<HostUpdateListener> hostUpdateListeners;
	// hostUpdateListeners = Arrays.asList(new HostUpdateListener[] { failureDetector });
	//
	// ServiceFailureDetector serviceFailureDetector;
	// serviceFailureDetector = new ServiceToHostAdapter(failureDetector, hostUpdateListeners);
	//
	// serviceFailureDetector.updateServices(TestServices.LIST);
	// return serviceFailureDetector;
	// }
}