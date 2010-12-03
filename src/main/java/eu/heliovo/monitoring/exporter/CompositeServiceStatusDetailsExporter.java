package eu.heliovo.monitoring.exporter;

import static java.util.Collections.unmodifiableList;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.heliovo.monitoring.model.ServiceStatusDetails;

/**
 * One ServiceStatusDetailsExporter containing many ServiceStatusDetailsExporter. This Class implements the Composite
 * Pattern, see http://en.wikipedia.org/wiki/Composite_pattern
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class CompositeServiceStatusDetailsExporter implements ServiceStatusDetailsExporter {

	private final List<ServiceStatusDetailsExporter> serviceStatusDetailsExporter;

	// Spring automatically injects all Exporter matching the interface ServiceStatusDetailsExporter, but not this class
	// itself.
	@Autowired
	public CompositeServiceStatusDetailsExporter(List<ServiceStatusDetailsExporter> exporter) {
		serviceStatusDetailsExporter = unmodifiableList(exporter);
	}

	@Override
	public void exportServiceStatusDetails(List<ServiceStatusDetails> serviceStatusDetails) {

		for (ServiceStatusDetailsExporter exporter : serviceStatusDetailsExporter) {
			exporter.exportServiceStatusDetails(serviceStatusDetails);
		}
	}
}