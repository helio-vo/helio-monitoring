package eu.heliovo.monitoring.statics;

import static eu.heliovo.monitoring.model.ModelFactory.newOperationTest;
import static eu.heliovo.monitoring.model.ModelFactory.newService;

import java.net.*;
import java.util.*;

import eu.heliovo.monitoring.model.*;

/**
 * 
 * Contains a static set of all monitored services with optional predefined requests and responses.
 * 
 * @author Kevin Seidler
 * 
 */
public final class Services {

	public static final Set<Service> LIST;

	private static final String MSSL_URL = "http://helio.mssl.ucl.ac.uk/";

	private Services() {
	}

	static {

		try {
			Set<Service> services = new HashSet<Service>();

			// taken from https://grid.ie/helio/wiki/HelioServicesDetails
			URL hecUrl = new URL("http://140.105.77.30:8080/helio-hec-r3/HelioService?wsdl");
			services.add(newService("HEC", "HEC", hecUrl, createHecMsslOperationTests()));
			services.add(newService("DPAS", "DPAS", new URL(MSSL_URL + "helio-dpas/HelioService?wsdl")));
			services.add(newService("ICS_R2", "ICS_R2", new URL(MSSL_URL + "helio-ics/HelioService?wsdl")));
			services.add(newService("ICS_R3", "ICS_R3", new URL(MSSL_URL + "helio-ics-r3/HelioService?wsdl")));
			services.add(newService("ILS", "ILS", new URL(MSSL_URL + "helio-ils/HelioService?wsdl")));
			services.add(newService("HRS", "HRS", new URL(MSSL_URL + "helio_registry/HelioService?wsdl")));
			services.add(newService("HFC", "HFC", new URL("http://helio-fc1.obspm.fr:8080/helio-hfc/HelioService?wsdl")));

			LIST = Collections.unmodifiableSet(services);

		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	private static List<OperationTest> createHecMsslOperationTests() {

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

	// TODO add working operation tests
	// private static List<OperationTest> createHelioWorkFlowSampleOperationTests() {
	//
	// final StringBuffer buffer = new StringBuffer();
	// buffer.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
	// buffer.append("xmlns:v0=\"http://helio-vo.eu/xml/WorkflowService/v0.1\">\n");
	// buffer.append("<soapenv:Header/>\n");
	// buffer.append("<soapenv:Body>\n");
	// buffer.append("<v0:InitialWorkflowParameters>\n");
	// buffer.append("<STARTTIME>2008-10-20T20:30:00</STARTTIME>\n");
	// buffer.append("<ENDTIME>2009-10-20T20:30:00</ENDTIME>\n");
	// buffer.append("</v0:InitialWorkflowParameters>\n");
	// buffer.append("</soapenv:Body>\n");
	// buffer.append("</soapenv:Envelope>");
	//
	// final OperationTest operationTest = newOperationTest("InitialWorkflow", buffer.toString());
	//
	// return Arrays.asList(operationTest);
	// }
	//
	// private static List<OperationTest> createFrontendFacadeSampleOperationTests() {
	//
	// final StringBuffer request1 = new StringBuffer();
	// request1.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
	// request1.append("xmlns:core=\"http://core.helio.i4ds.ch/\">\n");
	// request1.append("<soapenv:Header/>\n");
	// request1.append("<soapenv:Body>\n");
	// request1.append("<core:query_v1>\n");
	// request1.append("<max_results>5</max_results>\n");
	// request1.append("</core:query_v1>\n");
	// request1.append("</soapenv:Body>\n");
	// request1.append("</soapenv:Envelope>");
	//
	// final OperationTest operationTest1 = newOperationTest("query_v1", request1.toString());
	//
	// final StringBuffer request2 = new StringBuffer();
	// request2.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
	// request2.append("xmlns:core=\"http://core.helio.i4ds.ch/\">\n");
	// request2.append("<soapenv:Header/>\n");
	// request2.append("<soapenv:Body>\n");
	// request2.append("<core:get_host_name/>\n");
	// request2.append("</soapenv:Body>\n");
	// request2.append("</soapenv:Envelope>");
	//
	// final StringBuffer response2 = new StringBuffer();
	// response2.append("<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">");
	// response2.append("<S:Body>");
	// response2.append("<ns2:get_host_nameResponse xmlns:ns2=\"http://core.helio.i4ds.ch/\">");
	// response2.append("<return>helio.i4ds.technik.fhnw.ch</return>");
	// response2.append("</ns2:get_host_nameResponse>");
	// response2.append("</S:Body>");
	// response2.append("</S:Envelope>");
	//
	// final OperationTest operationTest2 = newOperationTest("get_host_name", request2.toString(),
	// response2.toString());
	//
	// return Arrays.asList(operationTest1, operationTest2);
	// }
}