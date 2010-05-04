package eu.heliovo.monitoring.daemon;

/**
 * Available states given by Nagios. Do not change the ordering, because the ordinals are used!
 * 
 * @author kevin
 * 
 */
public enum NagiosStatus {
	OK, WARNING, CRITICAL, UNKNOWN
}
