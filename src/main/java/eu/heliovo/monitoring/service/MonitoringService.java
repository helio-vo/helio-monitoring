package eu.heliovo.monitoring.service;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebService;

import eu.heliovo.monitoring.model.ServiceStatusDetails;

@WebService
public interface MonitoringService {

	/**
	 * Returning the actual ping status of the services.
	 * 
	 * @return List of service status
	 */
	@WebMethod
	List<ServiceStatusDetails> getPingStatus();

	@WebMethod
	List<ServiceStatusDetails> getMethodCallStatus();

	@WebMethod
	List<ServiceStatusDetails> getTestingStatus();
}