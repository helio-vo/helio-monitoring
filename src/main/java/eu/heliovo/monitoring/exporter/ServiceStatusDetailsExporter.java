package eu.heliovo.monitoring.exporter;

import java.util.List;

import eu.heliovo.monitoring.model.ServiceStatusDetails;

/**
 * Exports service status details to a destination.
 * 
 * @author Kevin Seidler
 * 
 */
public interface ServiceStatusDetailsExporter {

	void exportServiceStatusDetails(List<ServiceStatusDetails> serviceStatusDetails);

}