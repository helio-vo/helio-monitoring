package eu.heliovo.monitoring.failuredetector;

import org.junit.Ignore;

import eu.heliovo.monitoring.test.util.*;

@Ignore
public final class FailureDetectorTestUtils {

	private FailureDetectorTestUtils() {
	}

	public static ServiceFailureDetector getServiceFailureDetector() throws Exception {

		PhiAccrualFailureDetector phiAccrualFailureDetector = new PhiAccrualFailureDetector(TestUtils.getExecutor());
		ServiceFailureDetector serviceFailureDetector = new ServiceToHostAdapter(phiAccrualFailureDetector);
		serviceFailureDetector.updateServices(TestServices.LIST);

		phiAccrualFailureDetector.detect(); // detect at least one time and wait to have results for testing
		Thread.sleep(1500);
		phiAccrualFailureDetector.detect();
		Thread.sleep(1500);

		return serviceFailureDetector;
	}
}