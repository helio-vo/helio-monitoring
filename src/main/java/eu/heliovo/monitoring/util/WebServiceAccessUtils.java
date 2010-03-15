package eu.heliovo.monitoring.util;

import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.impl.wsdl.WsdlSubmit;
import com.eviware.soapui.impl.wsdl.WsdlSubmitContext;
import com.eviware.soapui.model.iface.Response;

/**
 * Utils for accessing web services.
 * 
 * @see http://www.soapui.org/architecture/integration.html
 * @author Kevin Seidler
 * 
 */
public class WebServiceAccessUtils {

	// TODO generalization of this code
	public void getAccess() throws Exception {
		// create new project
		final WsdlProject project = new WsdlProject();

		// import wsdl
		final WsdlInterface iface = project.importWsdl(
				"http://helio-dev.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl", true)[0];

		// get "GetPage" operation
		final WsdlOperation operation = iface.getOperationByName("get_version");

		// create a new empty request for that operation
		final WsdlRequest request = operation.addNewRequest("My request");

		// maybe for method parameters
		// request.fireIndexedPropertyChange(propertyName, index, oldValue,
		// newValue);

		// generate the request content from the schema
		request.setRequestContent(operation.createRequest(true));

		// submit the request
		final WsdlSubmit submit = request.submit(new WsdlSubmitContext(null), false);

		// wait for the response
		final Response response = submit.getResponse();

		// print the response
		final String content = response.getContentAsString();
		System.out.println(content);
		System.out.println(submit.getStatus());
		System.out.println(response.getTimeTaken());
		// assertNotNull(content);
		// assertTrue(content.indexOf("404 Not Found") > 0);

		// TODO compare received response with expected values
	}
}
