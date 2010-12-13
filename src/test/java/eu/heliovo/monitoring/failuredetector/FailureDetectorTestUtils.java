package eu.heliovo.monitoring.failuredetector;

import org.junit.Ignore;

import eu.heliovo.monitoring.test.util.TestUtils;

@Ignore
public final class FailureDetectorTestUtils {

	private FailureDetectorTestUtils() {
	}

	public static ServiceFailureDetector getServiceFailureDetector() throws Exception {
		return new ServiceToHostAdapter(new PhiAccrualFailureDetector(TestUtils.getExecutor()));
	}
}