package eu.heliovo.monitoring.stage;

import java.util.List;

import eu.heliovo.monitoring.model.*;

/**
 * TODO DOCUMENT ME
 * 
 * @author Kevin Seidler
 * 
 */
public interface MonitoringStage {

	void setServices(List<Service> services);

	void updateStatus();

	List<ServiceStatusDetails> getServicesStatus();
}