package eu.heliovo.monitoring.component;

import static eu.heliovo.monitoring.model.ServiceFactory.newServiceStatusDetails;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.ServiceStatusDetails;

/**
 * Just gets the WSDL file to see that the service is not offline. By convention the file is on the same host as the
 * service.
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
	 * Pings the Helio services, tracks online or offline status with a given timeout and measures response time.
	 */
	@Override
	public void refreshCache() {

		List<ServiceStatusDetails> newCache = new ArrayList<ServiceStatusDetails>();

		for (Service service : super.getServices()) {

			String serviceName = service.getName() + super.getServiceNameSuffix();

			StopWatch watch = new StopWatch(serviceName);
			URL url = service.getUrl();

			Exception exception = null;
			try {

				URLConnection connection = url.openConnection();
				connection.setConnectTimeout(TIMEOUT_IN_SECONDS);
				watch.start();
				connection.connect();
				watch.stop();

			} catch (Exception e) {
				exception = e;
			}

			if (exception != null) {

				int responseTimeInMillis = 0;
				ServiceStatus status = ServiceStatus.CRITICAL;
				String message = "an error occured: " + exception.getMessage();

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