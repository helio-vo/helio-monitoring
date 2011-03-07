package eu.heliovo.monitoring.failuredetector;

import junit.framework.Assert;

import org.junit.Test;

public class SamplingWindowTest extends Assert {

	@Test
	public void testSamplingWindow() {

		int numberOfMonitoredMeasures = 4;
		int intervalTimeInMillis = 1000;

		SamplingWindow samplingWindow = new SamplingWindow(numberOfMonitoredMeasures, intervalTimeInMillis);

		// with no values, phi should be at maximum, the host is down
		// assertEquals(Double.MAX_VALUE, samplingWindow.getPhi(111));

		// time is increasing, but response time stays nearly 111 ms
		// add 5 values (monitored measure size is 4) to delete first arbitrary value
		samplingWindow.addValue(111);
		samplingWindow.addValue(220);
		samplingWindow.addValue(333);
		samplingWindow.addValue(443);
		samplingWindow.addValue(556);

		assertEquals(0.137, samplingWindow.getPhi(666), 0.01);

		// with convict value of 8, host is marked as down
		// the failure reacts very sensitive here because there is nearly no standard deviation
		assertEquals(9.506, samplingWindow.getPhi(680), 0.01);

		// with increasing time, response time is increase by 111 ms, resulting in an increase of phi
		samplingWindow.addValue(777);
		samplingWindow.addValue(1110);
		samplingWindow.addValue(1554);
		samplingWindow.addValue(2109);

		assertEquals(3.631, samplingWindow.getPhi(3000), 0.01);
	}
}