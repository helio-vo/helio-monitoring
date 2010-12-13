package eu.heliovo.monitoring.component;

import java.util.List;

import eu.heliovo.monitoring.model.*;

/**
 * TODO DOCUMENT ME
 * 
 * @author Kevin Seidler
 * 
 */
public interface MonitoringComponent {

	void setServices(List<Service> services);

	void updateStatus();

	List<ServiceStatusDetails> getServicesStatus();
}