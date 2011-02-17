package eu.heliovo.monitoring.listener;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.util.ServiceHostUtils;

/**
 * This adapter listens to service updates, detemines their hosts and updates all host listeners.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class ServiceToHostAdapter implements ServiceUpdateListener {

	private final List<HostUpdateListener> hostUpdateListeners;

	@Autowired
	public ServiceToHostAdapter(List<HostUpdateListener> hostUpdateListeners) {
		this.hostUpdateListeners = Collections.unmodifiableList(hostUpdateListeners);
	}

	@Override
	public void updateServices(Set<Service> newServices) {
		Set<Host> newHosts = getHostsFromServices(newServices);
		updateHostUpdateListeners(newHosts);
	}

	private void updateHostUpdateListeners(Set<Host> newHosts) {
		for (HostUpdateListener listener : hostUpdateListeners) {
			listener.updateHosts(newHosts);
		}
	}

	protected Set<Host> getHostsFromServices(Set<Service> newServices) {
		return ServiceHostUtils.getHostsFromServices(newServices);
	}
}