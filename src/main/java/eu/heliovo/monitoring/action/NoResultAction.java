package eu.heliovo.monitoring.action;

/**
 * This action describes general actions which simply execute returning no result. Please see {@link Action} for more
 * details.
 * 
 * @author Kevin Seidler
 * 
 */
public interface NoResultAction extends Action {

	/**
	 * Executes the action.
	 */
	void execute();
}