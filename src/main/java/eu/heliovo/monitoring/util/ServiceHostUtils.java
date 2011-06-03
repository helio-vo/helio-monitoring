package eu.heliovo.monitoring.util;

import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import eu.heliovo.monitoring.model.*;

/**
 * Utility methods for dealing with service hosts.
 * 
 * @author Kevin Seidler
 * 
 */
public final class ServiceHostUtils {

	private ServiceHostUtils() {
	}

	public static Set<Host> getHostsFromServices(Set<Service> newServices) {

		Map<String, Set<Service>> hostsWithServices = assembleHostsWithServices(newServices);
		Set<Host> hosts = convertHostMapToSet(hostsWithServices);

		return hosts;
	}

	private static Set<Host> convertHostMapToSet(Map<String, Set<Service>> hostsWithServices) {
		// create a set of hosts from the map hostsWithServices
		Set<Host> hosts = new HashSet<Host>();
		for (Entry<String, Set<Service>> entry : hostsWithServices.entrySet()) {
			Set<Service> hostsServices = entry.getValue();
			Service anyService = hostsServices.iterator().next();
			hosts.add(ModelFactory.newHost(anyService.getUrl(), hostsServices));
		}
		return hosts;
	}

	private static Map<String, Set<Service>> assembleHostsWithServices(Set<Service> newServices) {

		Map<String, Set<Service>> hostsWithServices = new HashMap<String, Set<Service>>();

		// assemble hosts and their services using a map, because instances implementing the Host interface can only be
		// created with a complete list of services, which cannot be changed afterwards
		for (Service service : newServices) {

			URL serviceUrl = service.getUrl();
			String host = serviceUrl.getProtocol() + "://" + serviceUrl.getHost() + (serviceUrl.getPort() != 80 ? serviceUrl.getPort() : "");

			if (hostsWithServices.containsKey(host)) {
				Set<Service> hostsServices = hostsWithServices.get(host);
				hostsServices.add(service);
			} else {
				Set<Service> hostsServices = new HashSet<Service>();
				hostsServices.add(service);
				hostsWithServices.put(host, hostsServices);
			}
		}
		return hostsWithServices;
	}
}