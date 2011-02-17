package eu.heliovo.monitoring.stage;

import java.util.List;

import eu.heliovo.monitoring.listener.HostUpdateListener;
import eu.heliovo.monitoring.model.*;

/**
 * Executes monitoring stages with a certain order and interaction. For a concrete implementation see
 * {@link StageExecutorImpl}.
 * 
 * @author Kevin Seidler
 * 
 */
public interface StageExecutor extends HostUpdateListener {

	/**
	 * Executes the stages once.
	 */
	void execute();

	/**
	 * Continously executes all stages.
	 */
	void doContinousExecution();

	/**
	 * Returns the common result of all stage executions, which is the status of the monitored entities (hosts or
	 * services).
	 * 
	 * @return status
	 */
	List<StatusDetails<Service>> getStatus();
}