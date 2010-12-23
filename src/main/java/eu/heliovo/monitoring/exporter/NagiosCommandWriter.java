package eu.heliovo.monitoring.exporter;

import java.util.List;

/**
 * The {@link NagiosCommandWriter} assembles a Nagios command with its arguments and writes it to Nagios external
 * command file, for more information see: http://nagios.sourceforge.net/docs/3_0/extcommands.html
 * 
 * @author Kevin Seidler
 * 
 */
public interface NagiosCommandWriter {

	void write(NagiosCommand command, List<String> commandArguments);
}