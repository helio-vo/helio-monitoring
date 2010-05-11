package eu.heliovo.monitoring.model;

import java.net.URL;
import java.util.List;

public final class ServiceFactory {

	private ServiceFactory() {
	}

	public static Service newService(final String name, final URL url) {
		return new ServiceImpl(name, url);
	}

	public static ServiceWithRequests newService(final String name, final URL url, final List<String> requests) {
		return new ServiceWithRequestsImpl(name, url, requests);
	}
}
