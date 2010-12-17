package eu.heliovo.monitoring.failuredetector;

import java.util.List;

import eu.heliovo.monitoring.listener.ServiceUpdateListener;
import eu.heliovo.monitoring.model.ServiceStatusDetails;

/**
 * Detects the service status (OK or CRITICAL) of monitored services.
 * 
 * @author Kevin Seidler
 * 
 */
public interface ServiceFailureDetector extends ServiceUpdateListener {

	/**
	 * Returns detailed information about all monitored services adding a servce name suffix to every service name.
	 */
	List<ServiceStatusDetails> getServicesStatus(String serviceNameSuffix);
}