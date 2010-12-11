package eu.heliovo.monitoring.model;

import java.net.URL;
import java.util.*;

public final class HostImpl implements Host {

	private final URL url;
	private final List<Service> services;

	protected HostImpl(URL url, List<Service> services) {
		if (url == null) {
			throw new IllegalArgumentException("url must not be null!");
		}
		this.url = url;
		this.services = Collections.unmodifiableList(services);
	}

	@Override
	public String getName() {
		return url.getHost();
	}

	@Override
	public URL getUrl() {
		return url;
	}

	@Override
	public List<Service> getServices() {
		return services;
	}

}