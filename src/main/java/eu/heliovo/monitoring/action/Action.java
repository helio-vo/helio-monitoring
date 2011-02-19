package eu.heliovo.monitoring.action;

/**
 * This is a common interface for all actions. TODO DOCUMENT ME<br/>
 * TODO to log taken actions, see Design Patterns from Gamma et. al. at Pattern Command
 * 
 * @author Kevin Seidler
 * 
 * @param <T>
 */
public interface Action<T> {

	T getResult() throws Exception;
}