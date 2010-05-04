package eu.heliovo.monitoring.util;

import org.apache.xmlbeans.XmlException;

import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;
import com.eviware.soapui.impl.wsdl.support.soap.SoapUtils;
import com.eviware.soapui.impl.wsdl.support.soap.SoapVersion;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlContext;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlValidator;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlResponseMessageExchange;
import com.eviware.soapui.model.testsuite.AssertionError;

public class WsdlValidationUtils {

	public static AssertionError[] validateRequest(final WsdlRequest request) {

		final WsdlContext wsdlContext = request.getOperation().getInterface().getWsdlContext();
		final WsdlValidator validator = new WsdlValidator(wsdlContext);

		final WsdlResponseMessageExchange wsdlMessageExchange = new WsdlResponseMessageExchange(request);
		wsdlMessageExchange.setRequestContent(request.getRequestContent());

		return validator.assertRequest(wsdlMessageExchange, true);
	}

	public static AssertionError[] validateResponse(final WsdlResponse response) {

		final WsdlRequest request = response.getRequest();
		final WsdlContext wsdlContext = request.getOperation().getInterface().getWsdlContext();
		final WsdlValidator validator = new WsdlValidator(wsdlContext);

		final WsdlResponseMessageExchange wsdlMessageExchange = new WsdlResponseMessageExchange(request);
		wsdlMessageExchange.setResponse(response);

		return validator.assertResponse(wsdlMessageExchange, true);
	}

	public static boolean isSoapFault(final String responseContent, final WsdlOperation operation) throws XmlException {
		final SoapVersion soapVersion = operation.getInterface().getSoapVersion();
		return SoapUtils.isSoapFault(responseContent, soapVersion);
	}

}
