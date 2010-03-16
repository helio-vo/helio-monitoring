package eu.heliovo.monitoring.component;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.State;
import eu.heliovo.monitoring.util.ObjectCopyUtils;

@Component
public class PingComponent {

	// timeout in seconds
	private final static int TIMEOUT = 180;

	private Map<String, URL> services = new HashMap<String, URL>();

	// cache, could be improved through e.g. EhCache
	private List<ServiceStatus> cache = new ArrayList<ServiceStatus>();

	/**
	 * Just returning the actual ping status.
	 */
	public List<ServiceStatus> getStatus() {
		return (List<ServiceStatus>) ObjectCopyUtils.copyCollection(cache, new ArrayList<ServiceStatus>());
	}

	/**
	 * Pings the Helio services, tracks online or offline state with a given
	 * timeout and measures response time. This method gets executed
	 * autmatically by a quartz job from the application context.
	 */
	public void refreshCache() {

		final List<ServiceStatus> newCache = new ArrayList<ServiceStatus>();

		// this loop could be parallelized, but time meaturement could be
		// influenced
		for (final Entry<String, URL> service : services.entrySet()) {

			boolean exception = false;
			final StopWatch watch = new StopWatch(service.getKey());
			final URL url = service.getValue();

			try {

				final URLConnection connection = url.openConnection();
				connection.setConnectTimeout(TIMEOUT);
				watch.start();
				connection.connect();
				watch.stop();

				// problem: connection can be established if host is online, but
				// maybe the service is down => test this in method call?

			} catch (final IOException e) {
				exception = true;
			}

			if (exception) {
				newCache.add(new ServiceStatus(service.getKey(), url));
			} else {
				final int reponseTime = Double.valueOf(watch.getTotalTimeMillis()).intValue();
				newCache.add(new ServiceStatus(service.getKey(), url, State.UP, reponseTime));
			}
		}

		this.cache = newCache;
	}

	public void setServices(final Map<String, URL> services) {
		this.services = services;
	}
}
