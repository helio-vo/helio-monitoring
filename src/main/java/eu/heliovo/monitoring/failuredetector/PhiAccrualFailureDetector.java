package eu.heliovo.monitoring.failuredetector;

import java.io.IOException;
import java.net.URLConnection;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.heliovo.monitoring.model.Host;

/**
 * Implements the Phi Accrual Failure Detector proposes by Hayashibara et al. in the paper
 * "The Phi Accrual Failure Detector". This implemantation uses the query-response mechanism, also called
 * pull-mechanism. This is proposed in "A Hybrid Approach for Building Eventually Accurate Failure Detectors" by
 * Mostefaoui et al. The original Detector by Hayashibara uses a heartbeat- or pull meachanism, which means the
 * monitored processes are sending a heartbeat message to the monitor. With the query-response-mechanism the monitor
 * sends a query message to every monitored processe and awaits a response.
 * 
 * Please see the papers for more details.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class PhiAccrualFailureDetector implements FailureDetector {

	private static final int NUMBER_MONITORED_MEASURES = 1000;
	private static final int PHI_CONVICT_THRESHOLD = 8;
	private static final int INTERVAL_TIME_IN_MILLIES = 1000;

	private final Logger logger = Logger.getLogger(this.getClass());
	private final ExecutorService executor;
	private final Map<Host, SamplingWindow> statistics = new ConcurrentHashMap<Host, SamplingWindow>();

	@Autowired
	protected PhiAccrualFailureDetector(ExecutorService executor) {
		this.executor = executor;
	}

	@Override
	public void updateHosts(List<Host> newHosts) {
		removeOldServices(newHosts);
		addNewServices(newHosts);
	}

	private void addNewServices(List<Host> newHosts) {
		for (Host newHost : newHosts) {
			if (!statistics.containsKey(newHost)) {
				statistics.put(newHost, new SamplingWindow(NUMBER_MONITORED_MEASURES, INTERVAL_TIME_IN_MILLIES));
			}
		}
	}

	private void removeOldServices(List<Host> newHosts) {
		for (Host hostFromStatistics : statistics.keySet()) {
			if (!newHosts.contains(hostFromStatistics)) {
				statistics.remove(hostFromStatistics);
			}
		}
	}

	// TODO call this method every 1 second, use @Scheduled?
	protected void detect() {

		for (Entry<Host, SamplingWindow> serviceStatistics : statistics.entrySet()) {

			final Host host = serviceStatistics.getKey();
			final SamplingWindow statistic = serviceStatistics.getValue();

			recordMeasuresInSeparateThread(host, statistic);
		}
	}

	private void recordMeasuresInSeparateThread(final Host host, final SamplingWindow statistic) {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					long responseTimeInMillis = measureResponseTimeInMillies(host);
					statistic.addValue(System.currentTimeMillis());
					statistic.setLatestResponseTime(responseTimeInMillis);
				} catch (Exception e) { // if an error occurs while measure the response time, no value is added
					logger.debug(e.getMessage(), e);
				}
			}
		});
	}

	private long measureResponseTimeInMillies(Host host) throws IOException, InterruptedException {

		long millisBefore = System.currentTimeMillis();
		URLConnection connection = host.getUrl().openConnection();
		connection.connect();
		long milliesAfter = System.currentTimeMillis();

		return milliesAfter - millisBefore;
	}

	@Override
	public boolean isAlive(Host host) {

		SamplingWindow statistic = getStatistic(host);

		long tNow = System.currentTimeMillis();
		double phi = statistic.getPhi(tNow);

		return phi > PHI_CONVICT_THRESHOLD ? false : true;
	}

	@Override
	public long getResponseTimeInMillis(Host host) {
		SamplingWindow statistic = getStatistic(host);
		return statistic.getLatestResponseTimeInMillis();
	}

	private SamplingWindow getStatistic(Host host) {
		SamplingWindow statistic = statistics.get(host);
		if (statistic == null) {
			throw new IllegalArgumentException("host is unknown, please call updateHosts");
		}
		return statistics.get(host);
	}
}