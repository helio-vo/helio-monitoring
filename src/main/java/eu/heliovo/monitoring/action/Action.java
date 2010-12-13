package eu.heliovo.monitoring.action;

/**
 * TODO DOCUMENT ME
 * 
 * @author Kevin Seidler
 * 
 * @param <T>
 */
public interface Action<T> {

	T getResult() throws Exception;
}