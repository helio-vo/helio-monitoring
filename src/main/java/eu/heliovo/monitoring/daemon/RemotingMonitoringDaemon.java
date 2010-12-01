package eu.heliovo.monitoring.daemon;

import java.util.List;

import eu.heliovo.monitoring.model.ServiceStatusDetails;

public interface RemotingMonitoringDaemon {

	void writeServiceStatusToNagios(List<ServiceStatusDetails> serviceStatusDetails);

}