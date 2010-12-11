package eu.heliovo.monitoring.failuredetector;

import static eu.heliovo.monitoring.model.ModelFactory.newServiceStatusDetails;
import static eu.heliovo.monitoring.model.ServiceStatus.CRITICAL;
import static eu.heliovo.monitoring.model.ServiceStatus.OK;

import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.heliovo.monitoring.model.*;

/**
 * This Adapter wraps the PhiAccrualFailureDetector which only handles single hosts. Many services can be running on the
 * same host. This would lead to parallel connection attempts not needed, but wasting ressources.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class ServiceToHostAdapter implements ServiceFailureDetector {

	private final FailureDetector failureDetector;

	private List<Host> hosts = Collections.emptyList();

	@Autowired
	public ServiceToHostAdapter(FailureDetector failureDetector) {
		this.failureDetector = failureDetector;
	}

	@Override
	public void updateServices(List<Service> newServices) {
		this.hosts = getHostsFromServices(newServices);
		failureDetector.updateHosts(hosts);
	}

	@Override
	public List<ServiceStatusDetails> getServiceStatus() {

		List<ServiceStatusDetails> serviceStatus = new ArrayList<ServiceStatusDetails>();

		for (Host host : hosts) {

			boolean hostIsAlive = failureDetector.isAlive(host);
			long responseTimeInMillis = failureDetector.getResponseTimeInMillis(host);

			for (Service service : host.getServices()) {

				String serviceName = service.getName();
				URL serviceUrl = service.getUrl();
				ServiceStatusDetails details;

				if (hostIsAlive) {

					String message = "Service host is reachable - response time = " + responseTimeInMillis + " ms";
					details = newServiceStatusDetails(serviceName, serviceUrl, OK, responseTimeInMillis, message);
				} else {

					responseTimeInMillis = Long.MAX_VALUE; // to be able to compare services by response time
					String message = "Service is unreachable";

					details = newServiceStatusDetails(serviceName, serviceUrl, CRITICAL, responseTimeInMillis, message);
				}

				serviceStatus.add(details);
			}
		}

		return serviceStatus;
	}

	protected List<Host> getHostsFromServices(List<Service> newServices) {

		Map<String, List<Service>> hostsWithServices = new HashMap<String, List<Service>>();

		// assemble hosts and their services using a map, because instances implementing the Host interface can only be
		// created with a complete list of services, which cannot be changed afterwards
		for (Service service : newServices) {

			URL serviceUrl = service.getUrl();
			String host = serviceUrl.getHost();

			if (hostsWithServices.containsKey(serviceUrl.getHost())) {
				List<Service> hostsServices = hostsWithServices.get(host);
				hostsServices.add(service);
			} else {
				List<Service> hostsServices = new ArrayList<Service>();
				hostsServices.add(service);
				hostsWithServices.put(host, hostsServices);
			}
		}

		// create a List of hosts from the map hostsWithServices
		List<Host> hosts = new ArrayList<Host>();
		for (Entry<String, List<Service>> entry : hostsWithServices.entrySet()) {
			List<Service> hostsServices = entry.getValue();
			hosts.add(ModelFactory.newHost(hostsServices.get(0).getUrl(), hostsServices));
		}

		return hosts;
	}
}