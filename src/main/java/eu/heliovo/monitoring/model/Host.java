package eu.heliovo.monitoring.model;

import java.net.URL;
import java.util.Set;

/**
 * Represents a host running services that have to be monitored.
 * 
 * @author Kevin Seidler
 * 
 */
public interface Host {

	String getName();

	/**
	 * Returns a URL to get connected with the host.
	 */
	URL getUrl();

	/**
	 * Returns a set of Services the host is running.
	 */
	Set<Service> getServices();
}