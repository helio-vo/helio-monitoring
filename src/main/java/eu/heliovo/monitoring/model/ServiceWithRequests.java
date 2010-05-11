package eu.heliovo.monitoring.model;

import java.util.List;

/**
 * Service with predefined SOAP-Requests to be monitored.
 * 
 * @author Kevin Seidler
 * 
 */
public interface ServiceWithRequests extends Service {

	List<String> getRequests();
}