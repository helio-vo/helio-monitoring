package eu.heliovo.monitoring.stage;

import eu.heliovo.monitoring.model.Service;

/**
 * Just calls one method of every service to see that it is working.
 * 
 * @author Kevin Seidler
 * 
 */
public interface MethodCallStage extends MonitoringStage<Service> {
}