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

	String getName();

	URL getUrl();
}