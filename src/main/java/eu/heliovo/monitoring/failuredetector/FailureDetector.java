package eu.heliovo.monitoring.failuredetector;

import eu.heliovo.monitoring.listener.HostUpdateListener;
import eu.heliovo.monitoring.model.Host;

/**
 * The FailureDetector recognizes if a host is alive or not.
 * 
 * @author Kevin Seidler
 * 
 */
public interface FailureDetector extends HostUpdateListener {

	boolean isAlive(Host host);

	long getResponseTimeInMillis(Host host);
}