package eu.heliovo.monitoring.action;

/**
 * This action describes general actions which execute and return a result. Please see {@link Action} for more details.
 * 
 * @author Kevin Seidler
 * 
 * @param <ResultType> The Type of the result the action returns.
 */
public interface ResultAction<ResultType> extends Action {

	/**
	 * Executes the action and returns its result.
	 * 
	 * @return the result of executing this action.
	 * @throws Exception in case of an error
	 */
	ResultType getResult() throws Exception;
}