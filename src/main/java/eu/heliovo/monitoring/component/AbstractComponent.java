package eu.heliovo.monitoring.component;

import java.util.ArrayList;
import java.util.List;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.util.ObjectCopyUtils;

public abstract class AbstractComponent {

	// cache, could be improved through e.g. EhCache
	protected List<ServiceStatus> cache = new ArrayList<ServiceStatus>();

	protected List<Service> services = new ArrayList<Service>();

	public final String SERVICE_NAME_SUFFIX;

	public AbstractComponent(final String serviceNameSuffix) {
		this.SERVICE_NAME_SUFFIX = serviceNameSuffix;
	}

	public abstract void refreshCache();

	/**
	 * Just returning the actual status.
	 */
	public List<ServiceStatus> getStatus() {
		return (List<ServiceStatus>) ObjectCopyUtils.copyCollection(cache, new ArrayList<ServiceStatus>());
	}

	public void setServices(final List<Service> services) {
		this.services = services;
	}

}
