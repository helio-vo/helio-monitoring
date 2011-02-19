package eu.heliovo.monitoring.action;

import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;

import eu.heliovo.monitoring.logging.LogFileWriter;
import eu.heliovo.monitoring.model.Service;

/**
 * Processes the Response of a SOAP request which.
 * 
 * @author Kevin Seidler
 * 
 */
public class ProcessResponseAction implements NoResultAction {

	private final WsdlResponse response;
	private final String serviceName;
	private final Service service;
	private final LogFileWriter logFileWriter;

	public ProcessResponseAction(WsdlResponse response, String serviceName, Service service, LogFileWriter logFileWriter) {
		this.response = response;
		this.serviceName = serviceName;
		this.service = service;
		this.logFileWriter = logFileWriter;
	}

	@Override
	public void execute() {
		processResponse(response, serviceName, service, logFileWriter);
	}

	private void processResponse(WsdlResponse response, String serviceName, Service service, LogFileWriter logFileWriter) {

		long responseTime = response.getTimeTaken();
		logFileWriter.write("Response received, response time = " + responseTime + " ms");

		WsdlOperation operation = response.getRequest().getOperation();
		logFileWriter.write("=== Response Content for Operation \"" + operation.getName() + "\" ===");

		String content = response.getContentAsString();
		logFileWriter.write(content);
	}
}