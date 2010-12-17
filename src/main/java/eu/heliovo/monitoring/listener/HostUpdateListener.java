package eu.heliovo.monitoring.listener;

import java.util.List;

import eu.heliovo.monitoring.model.Host;

/**
 * If the monitored hosts change, they have to be updated in every component using these hosts. The
 * {@link HostUpdateListener} interface is called, when a component changes the list of hosts to be monitored following
 * the Observer Pattern (see http://en.wikipedia.org/wiki/Observer_pattern ). In Java it is called Listener Pattern.
 * 
 * @author Kevin Seidler
 * 
 */
public interface HostUpdateListener {

	/**
	 * Updates the internal list of hosts of the implementing component.
	 * 
	 * @param newHosts the new and complete list of hosts monitored
	 */
	void updateHosts(List<Host> newHosts);
}