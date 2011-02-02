package eu.heliovo.monitoring.exporter;

import java.util.List;

import eu.heliovo.monitoring.model.ServiceStatusDetails;

/**
 * Exports status details to a destination.
 * 
 * @author Kevin Seidler
 * 
 */
public interface StatusDetailsExporter {

	void exportHostStatusDetails(List<ServiceStatusDetails> serviceStatusDetails);

	void exportServiceStatusDetails(List<ServiceStatusDetails> serviceStatusDetails);
}