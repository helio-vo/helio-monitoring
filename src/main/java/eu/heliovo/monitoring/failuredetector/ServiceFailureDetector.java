package eu.heliovo.monitoring.failuredetector;

import java.util.List;

import eu.heliovo.monitoring.model.*;

/**
 * Detects the service status (OK or CRITICAL) of monitored services.
 * 
 * @author Kevin Seidler
 * 
 */
public interface ServiceFailureDetector {

	/**
	 * Update the services to be monitored.
	 */
	void updateServices(List<Service> newServices);

	/**
	 * Returns detailed information about all monitored services adding a servce name suffix to every service name.
	 */
	List<ServiceStatusDetails> getServicesStatus(String serviceNameSuffix);
}