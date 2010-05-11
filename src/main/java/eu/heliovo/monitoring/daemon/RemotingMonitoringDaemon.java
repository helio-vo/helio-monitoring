package eu.heliovo.monitoring.daemon;

import java.util.List;

import eu.heliovo.monitoring.model.ServiceStatus;

public interface RemotingMonitoringDaemon {

	abstract void writeServiceStatusToNagios(final List<ServiceStatus> serviceStatus);

}