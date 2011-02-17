package eu.heliovo.monitoring.exporter;

import java.util.List;

import eu.heliovo.monitoring.model.*;

/**
 * Exports status details of monitored entities like hosts or services to a destination.
 * 
 * @author Kevin Seidler
 * 
 */
public interface StatusDetailsExporter {

	void exportHostStatusDetails(List<StatusDetails<Host>> hostStatusDetails);

	void exportServiceStatusDetails(List<StatusDetails<Service>> serviceStatusDetails);
}