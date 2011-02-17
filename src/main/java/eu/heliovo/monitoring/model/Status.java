package eu.heliovo.monitoring.model;

/**
 * The Status of a monitored entity (host, service), e.g. OK or CRITICAL.
 * 
 * @author Kevin Seidler
 * 
 */
public enum Status {
	OK, CRITICAL, WARNING, UNKNOWN
}