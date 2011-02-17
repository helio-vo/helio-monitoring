package eu.heliovo.monitoring.service;

import java.util.List;

import javax.jws.*;

import eu.heliovo.monitoring.model.*;

/**
 * The Monitoring Service offers monitoring information about the HELIO services. It executes three stages (ping, method
 * call, testing) which result in different testing data. This Interface provides operations to ask for these
 * informations.
 * 
 * @author Kevin Seidler
 * 
 */
@WebService
public interface MonitoringService {

	/**
	 * Returning the actual status of the services.
	 * 
	 * @return List of service status
	 */
	@WebMethod
	List<StatusDetails<Service>> getStatus();

	/**
	 * Returns the current status of the service given by its ID
	 * 
	 * @param serviceId the identifier for this service
	 * @return the current status of the service
	 */
	@WebMethod
	StatusDetails<Service> getStatus(String serviceId);
}