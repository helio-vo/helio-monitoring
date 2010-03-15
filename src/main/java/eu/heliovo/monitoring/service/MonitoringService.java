package eu.heliovo.monitoring.service;

import java.util.List;

import eu.heliovo.monitoring.model.ServiceStatus;

// @WebService
public interface MonitoringService {

	/**
	 * Returning the actual ping status of the services.
	 * 
	 * @return List of service status
	 */
	// @WebMethod
	public abstract List<ServiceStatus> getPingStatus();
}