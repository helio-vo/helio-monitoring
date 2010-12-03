package eu.heliovo.monitoring.exporter;

import static eu.heliovo.monitoring.model.ServiceFactory.newServiceStatusDetails;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.ServiceStatusDetails;

public class CompositeServiceStatusDetailsExporterTest extends Assert {

	@Test
	public void testExportServiceStatusDetails() throws Exception {

		List<ServiceStatusDetailsExporter> exporterToCall = new ArrayList<ServiceStatusDetailsExporter>();

		final List<ServiceStatusDetails> detailsToExport = new ArrayList<ServiceStatusDetails>();
		detailsToExport.add(newServiceStatusDetails("1", new URL("http://123.456.234.32"), ServiceStatus.OK, 0, ""));
		detailsToExport.add(newServiceStatusDetails("2", new URL("http://123.456.234.32"), ServiceStatus.OK, 0, ""));

		final TestResult result = new TestResult();

		exporterToCall.add(new ServiceStatusDetailsExporter() {
			@Override
			public void exportServiceStatusDetails(List<ServiceStatusDetails> serviceStatusDetails) {
				result.exporter1Called = true;
				assertEquals(detailsToExport, serviceStatusDetails);
			}
		});

		exporterToCall.add(new ServiceStatusDetailsExporter() {
			@Override
			public void exportServiceStatusDetails(List<ServiceStatusDetails> serviceStatusDetails) {
				result.exporter2Called = true;
				assertEquals(detailsToExport, serviceStatusDetails);
			}
		});

		ServiceStatusDetailsExporter exporter = new CompositeServiceStatusDetailsExporter(exporterToCall);
		exporter.exportServiceStatusDetails(detailsToExport);

		assertTrue(result.exporter1Called);
		assertTrue(result.exporter2Called);
	}

	private static class TestResult {
		private boolean exporter1Called;
		private boolean exporter2Called;
	}
}
