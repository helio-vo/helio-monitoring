package eu.heliovo.monitoring.statics;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import eu.heliovo.monitoring.model.Service;

public class Services {

	public final static List<Service> list = new ArrayList<Service>();

	static {

		try {

			list.add(new Service("HEC", new URL("http://helio.i4ds.technik.fhnw.ch:8080/core/HECService?wsdl")));

			list.add(new Service("FrontendFacade", new URL(
					"http://helio.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl")));

			list.add(new Service("WorkflowsService", new URL(
					"http://helio.i4ds.technik.fhnw.ch/helio-wf/WorkflowsService?wsdl")));

			list.add(new Service("HEC", new URL("http://helio-dev.i4ds.technik.fhnw.ch:8080/core/HECService?wsdl")));

			list.add(new Service("FrontendFacade", new URL(
					"http://helio-dev.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl")));

			list.add(new Service("WorkflowsService", new URL(
					"http://helio-dev.i4ds.technik.fhnw.ch/helio-wf/WorkflowsService?wsdl")));

			list.add(new Service("helio-workflow", new URL(
					"http://helio-dev.i4ds.technik.fhnw.ch:8080/helio-workflow/SOAP?WSDL")));

			list.add(new Service("FakeOfflineService", new URL("http://123.43.121.11/")));

			list.add(new Service("NoWsdlOfflineService", new URL(
					"http://helio-dev.i4ds.technik.fhnw.ch/fakeofflineservice")));

		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

}