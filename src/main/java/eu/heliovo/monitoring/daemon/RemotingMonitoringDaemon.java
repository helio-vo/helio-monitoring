package eu.heliovo.monitoring.daemon;

import java.util.List;

import eu.heliovo.monitoring.model.ServiceStatus;

public interface RemotingMonitoringDaemon {

	void writeServiceStatusToNagios(List<ServiceStatus> serviceStatus);

}