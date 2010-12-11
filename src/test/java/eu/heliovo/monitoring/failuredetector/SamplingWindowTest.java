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

		// time is increasing, but response time stays 111 ms
		// add 5 values (monitored measure size is 4) to delete first arbitrary value
		samplingWindow.addValue(111);
		samplingWindow.addValue(222);
		samplingWindow.addValue(333);
		samplingWindow.addValue(444);
		samplingWindow.addValue(555);

		assertEquals(0.434, samplingWindow.getPhi(666), 0.01);
		assertEquals(9.566, samplingWindow.getPhi(3000), 0.01); // with convict value of 8, host is marked as down

		// with increasing time, response time is increase by 111 ms, resulting in an increase of phi
		samplingWindow.addValue(777);
		samplingWindow.addValue(1110);
		samplingWindow.addValue(1554);
		samplingWindow.addValue(2109);

		assertEquals(0.744, samplingWindow.getPhi(2775), 0.01);
	}
}
