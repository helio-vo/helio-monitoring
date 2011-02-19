package eu.heliovo.monitoring.model;

import java.net.URL;

/**
 * This interface describes a service in the context of monitoring. Every implementation has to override
 * {@link Object#hashCode()}, because it is used to identify changes in the list of monitored services to update it if
 * so.
 * 
 * @author Kevin Seidler
 * 
 */
public interface Service {

	/**
	 * Returns an unique identifier for this service.
	 * 
	 * @return unique identifier
	 */
	String getIdentifier();

	String getName();

	/**
	 * Returns the canonical name of the service to use it in file names or any other cases where whitespaces and
	 * special characters are not allowed.
	 */
	String getCanonicalName();

	URL getUrl();
}