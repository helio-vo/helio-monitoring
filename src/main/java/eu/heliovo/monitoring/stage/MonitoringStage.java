package eu.heliovo.monitoring.stage;

import java.util.List;

import eu.heliovo.monitoring.model.ServiceStatusDetails;

/**
 * TODO DOCUMENT ME
 * 
 * @author Kevin Seidler
 * 
 */
public interface MonitoringStage {

	void updateStatus();

	List<ServiceStatusDetails> getServicesStatus();
}