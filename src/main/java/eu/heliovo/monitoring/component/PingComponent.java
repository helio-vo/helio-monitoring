package eu.heliovo.monitoring.component;

import static eu.heliovo.monitoring.model.ModelFactory.newServiceStatusDetails;

import java.net.*;
import java.util.*;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import eu.heliovo.monitoring.model.*;

/**
 * Gets the WSDL file to see that the service is not offline. By convention the file is on the same host as the service.
 * Therefore it is not needed to search the WSDL file to find the service address.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class PingComponent extends AbstractComponent {

	private static final int TIMEOUT_IN_MILLIS = 300;

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
				connection.setConnectTimeout(TIMEOUT_IN_MILLIS);
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