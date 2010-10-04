package eu.heliovo.monitoring.statics;

import static eu.heliovo.monitoring.model.ServiceFactory.newOperationTest;
import static eu.heliovo.monitoring.model.ServiceFactory.newService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.heliovo.monitoring.model.OperationTest;
import eu.heliovo.monitoring.model.Service;

public final class Services {

	public static final List<Service> LIST;

	private static final String HELIO_DEV_HOSTNAME = "helio-dev.i4ds.technik.fhnw.ch";

	private Services() {
	}

	static {

		try {

			final List<Service> services = new ArrayList<Service>();

			// taken from https://grid.ie/helio/wiki/HelioDevelopment
			services.add(newService("HEC", new URL("http://helio.i4ds.technik.fhnw.ch:8080/core/HECService?wsdl")));
			services.add(newService("HEC_mssl",
					new URL("http://msslxw.mssl.ucl.ac.uk:8080/helio-hec/HelioService?wsdl"),
					createHecMsslOperationTest()));
			services.add(newService("DPAS", new URL("http://msslxw.mssl.ucl.ac.uk:8080/helio-dpas/HelioService?wsdl")));
			services.add(newService("ICS", new URL("http://msslxw.mssl.ucl.ac.uk:8080/helio-ics/HelioService?wsdl")));
			services.add(newService("ILS", new URL("http://msslxw.mssl.ucl.ac.uk:8080/helio-ils/HelioService?wsdl")));
			services.add(newService("HRS", new URL(
					"http://msslxw.mssl.ucl.ac.uk:8080/helio_registry/services/RegistryQueryv1_0?wsdl")));

			services.add(newService("FrontendFacade", new URL(
					"http://helio.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl"),
					createFrontendFacadeSampleOperationTest()));

			services.add(newService("WorkflowsService", new URL(
					"http://helio.i4ds.technik.fhnw.ch/helio-wf/WorkflowsService?wsdl")));

			services.add(newService("HEC", new URL("http://" + HELIO_DEV_HOSTNAME + ":8080/core/HECService?wsdl")));

			services.add(newService("FrontendFacade", new URL("http://" + HELIO_DEV_HOSTNAME
					+ ":8080/core/FrontendFacadeService?wsdl"), createFrontendFacadeSampleOperationTest()));

			services.add(newService("WorkflowsService", new URL("http://" + HELIO_DEV_HOSTNAME
					+ "/helio-wf/WorkflowsService?wsdl")));

			services.add(newService("helio-workflow", new URL("http://" + HELIO_DEV_HOSTNAME
					+ ":8080/helio-workflow/SOAP?WSDL"), createHelioWorkFlowSampleOperationTest()));
			 
			services.add(newService("FakeOfflineService", new URL("http://123.43.121.11/")));

			services.add(newService("NoWsdlOfflineService", new URL("http://" + HELIO_DEV_HOSTNAME
					+ "/fakeofflineservice")));

			LIST = Collections.unmodifiableList(services);

		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	private static final List<OperationTest> createHelioWorkFlowSampleOperationTest() {

		final StringBuffer buffer = new StringBuffer();
		buffer.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
		buffer.append("xmlns:v0=\"http://helio-vo.eu/xml/WorkflowService/v0.1\">\n");
		buffer.append("<soapenv:Header/>\n");
		buffer.append("<soapenv:Body>\n");
		buffer.append("<v0:InitialWorkflowParameters>\n");
		buffer.append("<STARTTIME>2008-10-20T20:30:00</STARTTIME>\n");
		buffer.append("<ENDTIME>2009-10-20T20:30:00</ENDTIME>\n");
		buffer.append("</v0:InitialWorkflowParameters>\n");
		buffer.append("</soapenv:Body>\n");
		buffer.append("</soapenv:Envelope>");

		final OperationTest operationTest = newOperationTest("InitialWorkflow", buffer.toString());

		return Arrays.asList(operationTest);
	}

	private static final List<OperationTest> createFrontendFacadeSampleOperationTest() {

		final StringBuffer request1 = new StringBuffer();
		request1.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
		request1.append("xmlns:core=\"http://core.helio.i4ds.ch/\">\n");
		request1.append("<soapenv:Header/>\n");
		request1.append("<soapenv:Body>\n");
		request1.append("<core:query_v1>\n");
		request1.append("<max_results>5</max_results>\n");
		request1.append("</core:query_v1>\n");
		request1.append("</soapenv:Body>\n");
		request1.append("</soapenv:Envelope>");

		final OperationTest operationTest1 = newOperationTest("query_v1", request1.toString());

		final StringBuffer request2 = new StringBuffer();
		request2.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
		request2.append("xmlns:core=\"http://core.helio.i4ds.ch/\">\n");
		request2.append("<soapenv:Header/>\n");
		request2.append("<soapenv:Body>\n");
		request2.append("<core:get_host_name/>\n");
		request2.append("</soapenv:Body>\n");
		request2.append("</soapenv:Envelope>");

		final StringBuffer response2 = new StringBuffer();
		response2.append("<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">");
		response2.append("<S:Body>");
		response2.append("<ns2:get_host_nameResponse xmlns:ns2=\"http://core.helio.i4ds.ch/\">");
		response2.append("<return>helio.i4ds.technik.fhnw.ch</return>");
		response2.append("</ns2:get_host_nameResponse>");
		response2.append("</S:Body>");
		response2.append("</S:Envelope>");

		final OperationTest operationTest2 = newOperationTest("get_host_name", request2.toString(), response2
				.toString());

		return Arrays.asList(operationTest1, operationTest2);
	}

	private static final List<OperationTest> createHecMsslOperationTest() {

		final StringBuffer request1 = new StringBuffer();

		request1.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
		request1.append("xmlns:v0=\"http://helio-vo.eu/xml/QueryService/v0.1\">\n");
		request1.append("<soapenv:Header/>\n");
		request1.append("<soapenv:Body>\n");
		request1.append("<v0:Query>\n");
		request1.append("<FROM>2010-01-01T00:00:00</FROM>\n");
		request1.append("</v0:Query>\n");
		request1.append("</soapenv:Body>\n");
		request1.append("</soapenv:Envelope>");

		return Arrays.asList(newOperationTest("Query", request1.toString()));
	}
}