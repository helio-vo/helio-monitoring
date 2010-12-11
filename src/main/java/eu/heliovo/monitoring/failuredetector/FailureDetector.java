package eu.heliovo.monitoring.failuredetector;

import java.util.List;

import eu.heliovo.monitoring.model.Host;

/**
 * The FailureDetector recognizes if a host is alive or not.
 * 
 * @author Kevin Seidler
 * 
 */
public interface FailureDetector {

	/**
	 * Updates the list of monitored hosts.
	 */
	void updateHosts(List<Host> hosts);

	boolean isAlive(Host host);

	long getResponseTimeInMillis(Host host);
}