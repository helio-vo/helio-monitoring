package eu.heliovo.monitoring.exporter;

import static eu.heliovo.monitoring.model.ModelFactory.newServiceStatusDetails;
import static eu.heliovo.monitoring.model.ServiceStatus.CRITICAL;
import static eu.heliovo.monitoring.model.ServiceStatus.OK;

import java.net.URL;
import java.util.*;

import junit.framework.Assert;

import org.junit.*;

import eu.heliovo.monitoring.model.ServiceStatusDetails;

public class NagiosServiceStatusDetailsExporterTest extends Assert {

	private final List<ServiceStatusDetails> serviceStatusDetails = new ArrayList<ServiceStatusDetails>();

	private class TestNagiosCommandWriter implements NagiosCommandWriter {

		@Override
		public void write(NagiosCommand command, List<String> commandArguments) {

			assertTrue(command.equals(NagiosCommand.PROCESS_SERVICE_CHECK_RESULT));

			String serviceName = commandArguments.get(1);
			boolean serviceNameFound = false;

			for (ServiceStatusDetails details : serviceStatusDetails) {
				if (details.getName().equals(serviceName)) {
					serviceNameFound = true;

					String hostName = commandArguments.get(0);
					String status = commandArguments.get(2);
					String statusMessage = commandArguments.get(3);

					NagiosServiceStatus expectedNagiosStatus = NagiosServiceStatus.valueOf(details.getStatus().name());

					assertEquals(details.getUrl().getHost(), hostName);
					assertEquals(String.valueOf(expectedNagiosStatus.ordinal()), status);
					assertEquals(details.getMessage(), statusMessage);

					break;
				}

			}
			assertTrue(serviceNameFound);
		}
	};

	@Before
	public void initTestServiceStatusDetails() throws Exception {

		String firstMessage = OK.name() + " - response time = " + 5 + " ms";
		String firstUrl = "http://helio.i4ds.technik.fhnw.ch:8080/core/HECService?wsdl";
		ServiceStatusDetails first = newServiceStatusDetails("HEC", new URL(firstUrl), OK, 5, firstMessage);

		URL secondUrl = new URL("http://helio.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl");
		String secondMessage = CRITICAL.name() + " - response time = " + 10 + " ms";
		ServiceStatusDetails second = newServiceStatusDetails("FrontendFacade", secondUrl, CRITICAL, 10, secondMessage);

		URL thirdUrl = new URL("http://helio-dev.i4ds.technik.fhnw.ch/helio-wf/WorkflowsService?wsdl");
		String thirdMessage = CRITICAL.name() + " - response time = " + 15 + " ms";
		String thirdServiceName = "helio-dev WorkflowsService";
		ServiceStatusDetails third = newServiceStatusDetails(thirdServiceName, thirdUrl, CRITICAL, 15, thirdMessage);

		serviceStatusDetails.add(first);
		serviceStatusDetails.add(second);
		serviceStatusDetails.add(third);

		assertEquals(3, serviceStatusDetails.size());
	}

	@Test
	public void testWriteServiceStatusToNagios() throws Exception {

		TestNagiosCommandWriter testCommandWriter = new TestNagiosCommandWriter();
		ServiceStatusDetailsExporter exporter = new NagiosServiceStatusDetailsExporter(testCommandWriter);

		exporter.exportServiceStatusDetails(serviceStatusDetails);
	}
}