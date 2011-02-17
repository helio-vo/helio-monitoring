package eu.heliovo.monitoring.stage;

import java.util.*;

import eu.heliovo.monitoring.model.StatusDetails;

/**
 * Describes a common interface for all monitoring stages.
 * 
 * @author Kevin Seidler
 * 
 */
public interface MonitoringStage<MonitoredEntity> {

	/**
	 * Resolves the status of given monitored entites.
	 * 
	 * @param monitoredEntitys
	 * @return status details of the given monitored entites
	 */
	List<StatusDetails<MonitoredEntity>> getStatus(Set<MonitoredEntity> monitoredEntitys);
}