package eu.heliovo.monitoring.failuredetector;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.*;
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
	 * Calculates the propability from t to infinity with: 1 - cumulative distribution function of the normal
	 * distribution.
	 */
	private double pLater(double t) {
		return pLaterNormalDistributed(t);
	}

	private double pLaterNormalDistributed(double t) {

		double mean = statistics.getMean();
		double standardDeviation = getStandardDeviation();

		NormalDistribution normalDistribution = new NormalDistributionImpl(mean, standardDeviation);

		try {
			double cumulativeProbability = getCumulativeProbability(t, normalDistribution);
			return 1.0 - cumulativeProbability;
		} catch (MathException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private double getCumulativeProbability(double t, NormalDistribution normalDistribution) throws MathException {

		double cumulativeProbability = normalDistribution.cumulativeProbability(t);

		// in some case cum. Prob. is greater than 1, which is impossible
		if (cumulativeProbability > 1) {
			cumulativeProbability = 1.0;
		}

		return cumulativeProbability;
	}

	private double getStandardDeviation() {

		double standardDeviation = statistics.getStandardDeviation();

		// cannot be zero, the NormalDistributionImpl throws an error in this case, because of division by zero
		standardDeviation = guaranteeNotZero(standardDeviation);

		return standardDeviation;
	}

	private double guaranteeNotZero(double standardDeviation) {
		if (standardDeviation == 0.0) {
			standardDeviation = 0.1;
		}
		return standardDeviation;
	}

	// exponential distribution just for testing
	// private double pLaterExponentialDistributed(double t) {
	// double mean = statistics.getMean();
	// double exponent = -t / mean;
	// return Math.pow(Math.E, exponent);
	// }
}