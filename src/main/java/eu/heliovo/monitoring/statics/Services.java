package eu.heliovo.monitoring.statics;

import static eu.heliovo.monitoring.model.ServiceFactory.newService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.heliovo.monitoring.model.Service;

public final class Services {

	public final static List<Service> LIST = new ArrayList<Service>();

	private Services() {
	}

	static {

		try {

			LIST.add(newService("HEC", new URL("http://helio.i4ds.technik.fhnw.ch:8080/core/HECService?wsdl")));

			LIST.add(newService("FrontendFacade", new URL(
					"http://helio.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl")));

			LIST.add(newService("WorkflowsService", new URL(
					"http://helio.i4ds.technik.fhnw.ch/helio-wf/WorkflowsService?wsdl")));

			LIST.add(newService("HEC", new URL("http://helio-dev.i4ds.technik.fhnw.ch:8080/core/HECService?wsdl")));

			LIST.add(newService("FrontendFacade", new URL(
					"http://helio-dev.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl")));

			LIST.add(newService("WorkflowsService", new URL(
					"http://helio-dev.i4ds.technik.fhnw.ch/helio-wf/WorkflowsService?wsdl")));

			LIST.add(newService("helio-workflow", new URL(
					"http://helio-dev.i4ds.technik.fhnw.ch:8080/helio-workflow/SOAP?WSDL"),
					createHelioWorkFlowSampleRequests()));

			LIST.add(newService("FakeOfflineService", new URL("http://123.43.121.11/")));

			LIST.add(newService("NoWsdlOfflineService", new URL(
					"http://helio-dev.i4ds.technik.fhnw.ch/fakeofflineservice")));

		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	private final static List<String> createHelioWorkFlowSampleRequests() {

		final StringBuffer buffer = new StringBuffer();
		buffer.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
		buffer.append("xmlns:v0=\"http://helio-vo.eu/xml/WorkflowService/v0.1\">");
		buffer.append("<soapenv:Header/>");
		buffer.append("<soapenv:Body>");
		buffer.append("<v0:InitialWorkflowParameters>");
		buffer.append("<STARTTIME>12345</STARTTIME>");
		buffer.append("<ENDTIME>12345</ENDTIME>");
		buffer.append("</v0:InitialWorkflowParameters>");
		buffer.append("</soapenv:Body>");
		buffer.append("</soapenv:Envelope>");

		return Arrays.asList(buffer.toString());
	}

}