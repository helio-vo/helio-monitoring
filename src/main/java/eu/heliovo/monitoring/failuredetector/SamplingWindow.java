package eu.heliovo.monitoring.failuredetector;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 * Records interval samples and calculates phi.
 * 
 * @author Kevin Seidler
 * 
 */
public final class SamplingWindow {

	private final DescriptiveStatistics statistics;
	private final int intervalTimeInMillis;

	private long tLast = 0;
	private long latestResponseTimeInMillis = Long.MAX_VALUE;

	protected SamplingWindow(int numberOfMonitoredMeasures, int intervalTimeInMillis) {
		this.statistics = new DescriptiveStatistics(numberOfMonitoredMeasures);
		this.intervalTimeInMillis = intervalTimeInMillis;
	}

	// oldest values will be removed if number of values greater than NUMBER_MONITORED_MEASURES
	protected synchronized void addValue(long value) {

		long interArrivalTime = tLast > 0 ? value - tLast : intervalTimeInMillis / 2;
		tLast = value;
		statistics.addValue(interArrivalTime);
	}

	protected synchronized void setLatestResponseTime(long latestResponseTimeInMillis) {
		this.latestResponseTimeInMillis = latestResponseTimeInMillis;
	}

	protected synchronized long getLatestResponseTimeInMillis() {
		return latestResponseTimeInMillis;
	}

	/**
	 * Calculates phi as described in the paper of Hayashibara et al.
	 */
	protected synchronized double getPhi(long tNow) {

		long availableSamples = statistics.getN();
		double phi = Double.MAX_VALUE; // if no samples are available, phi exeeds convict threshold and the host is down
		if (availableSamples > 0) {
			phi = -Math.log10(pLater(tNow - tLast));
		}
		return phi;
	}

	/**
	 * Calculates the propability from t to infinity with: 1 - cumulative distribution function of the exponential
	 * distribution.
	 */
	private double pLater(double t) {
		double mean = statistics.getMean();
		double exponent = -t / mean;
		return Math.pow(Math.E, exponent);
	}

	// TODO use normal distribution as originally proposed?
	// private double pLaterNormalDistribution(double t) {
	//
	// double mean = statistics.getMean();
	// double standardDeviation = statistics.getStandardDeviation();
	//
	// if (standardDeviation == 0.0) {
	// standardDeviation = 0.000000000000000000000000000000001;
	// }
	//
	// System.out.println(mean + " std: " + standardDeviation);
	// NormalDistribution normalDistribution = new NormalDistributionImpl(mean, standardDeviation);
	//
	// try {
	// return 1 - normalDistribution.cumulativeProbability(t);
	// } catch (MathException e) {
	// throw new IllegalStateException(e.getMessage(), e);
	// }
	// }
}