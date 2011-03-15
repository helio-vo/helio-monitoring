package eu.heliovo.monitoring.action;

import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;

import eu.heliovo.monitoring.logging.LogFileWriter;

/**
 * Processes the response of a SOAP request by logging its content and response time.
 * 
 * @author Kevin Seidler
 * 
 */
public final class ProcessResponseAction implements NoResultAction {

	private final WsdlResponse response;
	private final LogFileWriter logFileWriter;

	public ProcessResponseAction(WsdlResponse response, LogFileWriter logFileWriter) {
		this.response = response;
		this.logFileWriter = logFileWriter;
	}

	@Override
	public void execute() {
		processResponse(response, logFileWriter);
	}

	private void processResponse(WsdlResponse response, LogFileWriter logFileWriter) {

		long responseTime = response.getTimeTaken();
		logFileWriter.write("Response received, response time = " + responseTime + " ms");

		WsdlOperation operation = response.getRequest().getOperation();
		logFileWriter.write("=== Response Content for Operation \"" + operation.getName() + "\" ===");

		String content = response.getContentAsString();
		logFileWriter.write(content);
	}
}