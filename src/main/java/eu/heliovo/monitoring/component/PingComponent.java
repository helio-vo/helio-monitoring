package eu.heliovo.monitoring.component;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.State;

@Component
public class PingComponent extends AbstractComponent {

	// timeout in seconds
	private final int TIMEOUT = 300;

	public PingComponent() {
		super(" -ping-");
	}

	/**
	 * Pings the Helio services, tracks online or offline state with a given
	 * timeout and measures response time. This method gets executed
	 * autmatically by a quartz job from the application context.
	 */
	@Override
	public void refreshCache() {

		final List<ServiceStatus> newCache = new ArrayList<ServiceStatus>();

		// this loop could be parallelized, but time meaturement could be
		// influenced
		for (final Service service : super.services) {

			final String serviceName = service.getName() + SERVICE_NAME_SUFFIX;

			final StopWatch watch = new StopWatch(serviceName);
			final URL url = service.getUrl();

			Exception exception = null;
			try {

				final URLConnection connection = url.openConnection();
				connection.setConnectTimeout(TIMEOUT);
				watch.start();
				connection.connect();
				watch.stop();

				// TODO problem: connection can be established if host is
				// online, but returns 404 for the wsdl file

			} catch (final Exception e) {
				exception = e;
			}

			if (exception != null) {
				final ServiceStatus status = new ServiceStatus(serviceName, url, State.DOWN, 0);
				status.setMessage("an error occured: " + exception.getMessage());
				newCache.add(status);
			} else {
				final int reponseTime = Double.valueOf(watch.getTotalTimeMillis()).intValue();

				final ServiceStatus status = new ServiceStatus(serviceName, url, State.UP, reponseTime);
				status.setMessage(status.getState().name() + " - response time = " + reponseTime + " ms");

				newCache.add(status);
			}
		}

		this.cache = newCache;
	}
}
