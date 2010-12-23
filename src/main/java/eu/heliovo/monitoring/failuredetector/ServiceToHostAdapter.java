package eu.heliovo.monitoring.failuredetector;

import static eu.heliovo.monitoring.model.ModelFactory.newServiceStatusDetails;
import static eu.heliovo.monitoring.model.ServiceStatus.CRITICAL;
import static eu.heliovo.monitoring.model.ServiceStatus.OK;

import java.net.URL;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.heliovo.monitoring.listener.*;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.util.ServiceHostUtils;

/**
 * This Adapter wraps the {@link PhiAccrualFailureDetector} which only handles single hosts. Many services can be
 * running on the same host. This would lead to parallel connection attempts not needed, but wasting resources.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class ServiceToHostAdapter implements ServiceFailureDetector, ServiceUpdateListener {

	private final FailureDetector failureDetector;
	private final List<HostUpdateListener> hostUpdateListeners;

	private Set<Host> hosts = Collections.emptySet();

	@Autowired
	public ServiceToHostAdapter(FailureDetector failureDetector, List<HostUpdateListener> hostUpdateListeners) {
		this.failureDetector = failureDetector;
		this.hostUpdateListeners = Collections.unmodifiableList(hostUpdateListeners);
	}

	@Override
	public void updateServices(Set<Service> newServices) {
		this.hosts = getHostsFromServices(newServices);
		updateHostUpdateListeners();
	}

	private void updateHostUpdateListeners() {
		for (HostUpdateListener listener : hostUpdateListeners) {
			listener.updateHosts(hosts);
		}
	}

	@Override
	public List<ServiceStatusDetails> getServicesStatus(String serviceNameSuffix) {

		List<ServiceStatusDetails> servicesStatus = new ArrayList<ServiceStatusDetails>();

		for (Host host : hosts) {

			boolean hostIsAlive = failureDetector.isAlive(host);
			long responseTimeInMillis = failureDetector.getResponseTimeInMillis(host);

			for (Service service : host.getServices()) {

				String serviceName = service.getName() + serviceNameSuffix;
				URL serviceUrl = service.getUrl();
				ServiceStatusDetails details;

				if (hostIsAlive) {

					String message = "Service host is reachable, response time = " + responseTimeInMillis + " ms";
					details = newServiceStatusDetails(serviceName, serviceUrl, OK, responseTimeInMillis, message);
				} else {

					responseTimeInMillis = Long.MAX_VALUE; // to be able to compare services by response time
					String message = "Service is unreachable";

					details = newServiceStatusDetails(serviceName, serviceUrl, CRITICAL, responseTimeInMillis, message);
				}

				servicesStatus.add(details);
			}
		}

		return servicesStatus;
	}

	protected Set<Host> getHostsFromServices(Set<Service> newServices) {
		return ServiceHostUtils.getHostsFromServices(newServices);
	}
}