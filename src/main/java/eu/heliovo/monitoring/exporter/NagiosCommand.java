package eu.heliovo.monitoring.exporter;

/**
 * These commands can be send to Nagios via its external command file. The commands defined here are only those the
 * monitoring service needs. The hole command list can be found at:
 * http://old.nagios.org/developerinfo/externalcommands/commandlist.php <br/>
 * Information about external commands can be found at: http://nagios.sourceforge.net/docs/3_0/extcommands.html
 * 
 * @author Kevin Seidler
 * 
 */
public enum NagiosCommand {
	PROCESS_SERVICE_CHECK_RESULT, RESTART_PROGRAM
}