package eu.heliovo.monitoring.component;

import static eu.heliovo.monitoring.model.ServiceFactory.newServiceStatusDetails;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatusDetails;
import eu.heliovo.monitoring.model.ServiceStatus;

/**
 * Just gets the WSDL file to see that the service is not offline.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class PingComponent extends AbstractComponent {

	private static final int TIMEOUT_IN_SECONDS = 300;

	public PingComponent() {
		super(" -ping-");
	}

	/**
	 * Pings the Helio services, tracks online or offline status with a given timeout and measures response time. This
	 * method gets executed autmatically by a quartz job from the application context.
	 */
	@Override
	public void refreshCache() {

		List<ServiceStatusDetails> newCache = new ArrayList<ServiceStatusDetails>();

		for (final Service service : super.getServices()) {

			final String serviceName = service.getName() + super.getServiceNameSuffix();

			final StopWatch watch = new StopWatch(serviceName);
			final URL url = service.getUrl();

			Exception exception = null;
			try {

				URLConnection connection = url.openConnection();
				connection.setConnectTimeout(TIMEOUT_IN_SECONDS);
				watch.start();
				connection.connect();
				watch.stop();

			} catch (final Exception e) {
				exception = e;
			}

			if (exception != null) {

				String message = "an error occured: " + exception.getMessage();
				int responseTimeInMillis = 0;
				ServiceStatus status = ServiceStatus.CRITICAL;

				newCache.add(newServiceStatusDetails(serviceName, url, status, responseTimeInMillis, message));

			} else {

				int responseTimeInMillis = Double.valueOf(watch.getTotalTimeMillis()).intValue();
				ServiceStatus status = ServiceStatus.OK;
				String message = status.name() + " - response time = " + responseTimeInMillis + " ms";

				newCache.add(newServiceStatusDetails(serviceName, url, status, responseTimeInMillis, message));
			}
		}

		super.setCache(newCache);
	}
}
