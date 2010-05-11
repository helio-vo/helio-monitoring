package eu.heliovo.monitoring.component;

import java.util.Collections;
import java.util.List;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;

public abstract class AbstractComponent {

	// cache, could be improved through e.g. EhCache
	private List<ServiceStatus> cache = Collections.emptyList();

	private List<Service> services = Collections.emptyList();

	private final String serviceNameSuffix;

	public AbstractComponent(final String serviceNameSuffix) {
		this.serviceNameSuffix = serviceNameSuffix;
	}

	public abstract void refreshCache();

	/**
	 * Just returning the actual status.
	 */
	public final List<ServiceStatus> getStatus() {
		return cache;
	}

	public final List<Service> getServices() {
		return services;
	}

	public final void setServices(final List<Service> services) {
		this.services = Collections.unmodifiableList(services);
	}

	public void setCache(final List<ServiceStatus> cache) {
		this.cache = Collections.unmodifiableList(cache);
	}

	public final String getServiceNameSuffix() {
		return serviceNameSuffix;
	}

}
