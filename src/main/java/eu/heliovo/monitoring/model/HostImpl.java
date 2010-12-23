package eu.heliovo.monitoring.model;

import java.net.URL;
import java.util.*;

public final class HostImpl implements Host {

	private final URL url;
	private final Set<Service> services;

	protected HostImpl(URL url, Set<Service> services) {
		if (url == null) {
			throw new IllegalArgumentException("url must not be null!");
		}
		this.url = url;
		this.services = Collections.unmodifiableSet(services);
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
	public Set<Service> getServices() {
		return services;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((services == null) ? 0 : services.hashCode());
		result = prime * result + ((url == null) ? 0 : url.toString().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		HostImpl other = (HostImpl) obj;
		if (services == null) {
			if (other.services != null) {
				return false;
			}
		} else if (!services.equals(other.services)) {
			return false;
		}
		if (url == null) {
			if (other.url != null) {
				return false;
			}
		} else if (!url.toString().equals(other.url.toString())) {
			return false;
		}
		return true;
	}
}