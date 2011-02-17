package eu.heliovo.monitoring.stage;

import eu.heliovo.monitoring.model.Host;

/**
 * The ping stage detects failures of the service hosts. See {@link PingStageImpl} for more details.
 * 
 * @author Kevin Seidler
 * 
 */
public interface PingStage extends MonitoringStage<Host> {
}