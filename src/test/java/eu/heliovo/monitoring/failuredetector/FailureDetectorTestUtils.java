package eu.heliovo.monitoring.failuredetector;

import java.util.*;

import org.junit.Ignore;

import eu.heliovo.monitoring.listener.HostUpdateListener;
import eu.heliovo.monitoring.model.Host;
import eu.heliovo.monitoring.test.util.*;

@Ignore
public final class FailureDetectorTestUtils {

	private FailureDetectorTestUtils() {
	}

	public static HostStatisticsRecorder getEmptyStatisticsRecorder() {
		return new HostStatisticsRecorder() {
			@Override
			public void updateHosts(List<Host> newHosts) {
			}

			@Override
			public long getNextEntryId(Host host) {
				return 0;
			}

			@Override
			public void record(Host host, long entryId, long measure) {
			}
		};
	}

	public static ServiceFailureDetector getServiceFailureDetector() throws Exception {

		PhiAccrualFailureDetector failureDetector;
		failureDetector = new PhiAccrualFailureDetector(TestUtils.getExecutor(), getEmptyStatisticsRecorder());

		ServiceFailureDetector serviceFailureDetector = initServiceFailureDetector(failureDetector);
		detectForResults(failureDetector);

		return serviceFailureDetector;
	}

	private static ServiceFailureDetector initServiceFailureDetector(PhiAccrualFailureDetector failureDetector) {

		List<HostUpdateListener> hostUpdateListeners;
		hostUpdateListeners = Arrays.asList(new HostUpdateListener[] { failureDetector });

		ServiceFailureDetector serviceFailureDetector;
		serviceFailureDetector = new ServiceToHostAdapter(failureDetector, hostUpdateListeners);

		serviceFailureDetector.updateServices(TestServices.LIST);
		return serviceFailureDetector;
	}

	private static void detectForResults(PhiAccrualFailureDetector failureDetector) throws InterruptedException {
		failureDetector.detect(); // detect at least one time and wait to have results for testing
		Thread.sleep(1500);
		failureDetector.detect();
		Thread.sleep(1500);
	}
}