package eu.heliovo.monitoring.listener;

import java.util.List;

import eu.heliovo.monitoring.model.Service;

/**
 * If the monitored services change, they have to be updated in every component using these services. The
 * {@link ServiceUpdateListener} interface is called, when a component changes the list of services to be monitored
 * following the Observer Pattern (see http://en.wikipedia.org/wiki/Observer_pattern ). In Java it is called Listener
 * Pattern.
 * 
 * @author Kevin Seidler
 * 
 */
public interface ServiceUpdateListener {

	/**
	 * Updates the internal list of services of the implementing component.
	 * 
	 * @param newServices the new and complete list of services monitored
	 */
	void updateServices(List<Service> newServices);
}