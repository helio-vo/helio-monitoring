package eu.heliovo.monitoring.action;

import com.eviware.soapui.impl.wsdl.*;
import com.eviware.soapui.model.testsuite.AssertionError;

import eu.heliovo.monitoring.logging.*;
import eu.heliovo.monitoring.util.WsdlValidationUtils;

/**
 * Creates a SOAP request to be send to a web service.
 * 
 * @author Kevin Seidler
 * 
 */
public final class CreateRequestAction implements ResultAction<WsdlRequest> {

	private static final boolean GENERATE_OPTINAL_PARAMS = false;

	private final LogFileWriter logFileWriter;
	private final WsdlOperation operation;
	private final String requestContent;

	public CreateRequestAction(LogFileWriter logFileWriter, WsdlOperation operation) {
		this(logFileWriter, operation, getRequestContent(operation));
	}

	private static String getRequestContent(WsdlOperation operation) {
		return operation.createRequest(GENERATE_OPTINAL_PARAMS);
	}

	public CreateRequestAction(LogFileWriter logFileWriter, WsdlOperation operation,
			String requestContent) {

		this.logFileWriter = logFileWriter;
		this.operation = operation;
		this.requestContent = requestContent;
	}

	@Override
	public WsdlRequest getResult() {
		return createRequest(logFileWriter, operation, requestContent);
	}

	private WsdlRequest createRequest(LogFileWriter logFileWriter,
			WsdlOperation operation, String requestContent) {

		WsdlRequest request = operation.addNewRequest(operation.getName());

		// generate the request content from the schema
		request.setRequestContent(requestContent);

		logFileWriter.write("=== Request Content for Operation \"" + operation.getName() + "\" ===");
		logFileWriter.write(request.getRequestContent());

		// validate request
		AssertionError[] requestAssertionErrors = WsdlValidationUtils.validateRequest(request);
		LoggingHelper.logRequestValidation(requestAssertionErrors, logFileWriter);

		return request;
	}
}