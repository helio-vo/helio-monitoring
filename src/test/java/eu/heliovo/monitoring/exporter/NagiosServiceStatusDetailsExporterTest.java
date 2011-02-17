package eu.heliovo.monitoring.exporter;

import static eu.heliovo.monitoring.model.ModelFactory.newStatusDetails;
import static eu.heliovo.monitoring.model.Status.CRITICAL;
import static eu.heliovo.monitoring.model.Status.OK;

import java.net.*;
import java.util.*;

import junit.framework.Assert;

import org.junit.*;

import eu.heliovo.monitoring.model.*;

public class NagiosServiceStatusDetailsExporterTest extends Assert {

	private final List<StatusDetails<Service>> serviceStatusDetails = new ArrayList<StatusDetails<Service>>();
	private final List<StatusDetails<Host>> hostStatusDetails = new ArrayList<StatusDetails<Host>>();

	private class TestServiceNagiosCommandWriter implements NagiosCommandWriter {

		@Override
		public void write(NagiosCommand command, List<String> commandArguments) {

			assertTrue(command.equals(NagiosCommand.PROCESS_SERVICE_CHECK_RESULT));

			String serviceName = commandArguments.get(1);
			boolean serviceNameFound = false;

			for (StatusDetails<Service> details : serviceStatusDetails) {
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

	private class TestHostNagiosCommandWriter implements NagiosCommandWriter {

		@Override
		public void write(NagiosCommand command, List<String> commandArguments) {

			assertTrue(command.equals(NagiosCommand.PROCESS_HOST_CHECK_RESULT));

			boolean firstHostFound = false;
			boolean secondHostFound = false;

			String hostName = commandArguments.get(0);

			if (hostName.equals("helio.i4ds.ch")) {
				firstHostFound = true;
			}

			if (hostName.equals("msslxw.mssl.ucl.ac.uk")) {
				secondHostFound = true;
			}

			for (StatusDetails<Host> details : hostStatusDetails) {

				if (hostName.equals(details.getUrl().getHost())) {

					String status = commandArguments.get(1);
					String statusMessage = commandArguments.get(2);

					NagiosServiceStatus expectedNagiosStatus = NagiosServiceStatus.valueOf(details.getStatus().name());

					assertEquals(String.valueOf(expectedNagiosStatus.ordinal()), status);
					assertEquals(details.getMessage(), statusMessage);

					break;
				}
			}

			assertTrue(firstHostFound || secondHostFound);
		}
	};

	@Before
	public void initTestStatusDetails() throws Exception {

		initTestServiceStatusDetails();
		initTestHostStatusDetails();
	}

	private void initTestServiceStatusDetails() throws MalformedURLException {

		String firstMessage = OK.name() + " - response time = " + 5 + " ms";
		String firstUrl = "http://helio.i4ds.technik.fhnw.ch:8080/core/HECService?wsdl";
		StatusDetails<Service> first = newStatusDetails(null, "HEC", new URL(firstUrl), OK, 5, firstMessage);

		URL secondUrl = new URL("http://helio.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl");
		String secondMessage = CRITICAL.name() + " - response time = " + 10 + " ms";
		StatusDetails<Service> second = newStatusDetails(null, "FrontendFacade", secondUrl, CRITICAL, 10, secondMessage);

		URL thirdUrl = new URL("http://helio-dev.i4ds.technik.fhnw.ch/helio-wf/WorkflowsService?wsdl");
		String thirdMessage = CRITICAL.name() + " - response time = " + 15 + " ms";
		String thirdServiceName = "helio-dev WorkflowsService";
		StatusDetails<Service> third = newStatusDetails(null, thirdServiceName, thirdUrl, CRITICAL, 15, thirdMessage);

		serviceStatusDetails.add(first);
		serviceStatusDetails.add(second);
		serviceStatusDetails.add(third);

		assertEquals(3, serviceStatusDetails.size());
	}
	
	private void initTestHostStatusDetails() throws Exception {
		
		String firstMessage = "Host is reachable, response time = " + 5 + " ms";
		String firstUrl = "http://helio.i4ds.ch:8080/core/HECService?wsdl";
		StatusDetails<Host> first = newStatusDetails(null, "HEC", new URL(firstUrl), OK, 5, firstMessage);

		URL secondUrl = new URL("http://msslxw.mssl.ucl.ac.uk:8080/core/FrontendFacadeService?wsdl");
		String secondMessage = "Host not reachable";
		StatusDetails<Host> second = newStatusDetails(null, "FrontendFacade", secondUrl, CRITICAL, 0, secondMessage);
		
		hostStatusDetails.add(first);
		hostStatusDetails.add(second);

		assertEquals(2, hostStatusDetails.size());
	}

	@Test
	public void testWriteServiceStatusToNagios() throws Exception {

		TestServiceNagiosCommandWriter testCommandWriter = new TestServiceNagiosCommandWriter();
		StatusDetailsExporter exporter = new NagiosServiceStatusDetailsExporter(testCommandWriter);

		exporter.exportServiceStatusDetails(serviceStatusDetails);
	}
	
	@Test
	public void testWriteHostStatusToNagios() throws Exception {

		TestHostNagiosCommandWriter testCommandWriter = new TestHostNagiosCommandWriter();
		StatusDetailsExporter exporter = new NagiosServiceStatusDetailsExporter(testCommandWriter);

		exporter.exportHostStatusDetails(hostStatusDetails);
	}
}