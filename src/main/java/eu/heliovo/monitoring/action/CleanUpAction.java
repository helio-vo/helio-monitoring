package eu.heliovo.monitoring.action;

import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.support.http.HttpClientSupport;

import eu.heliovo.monitoring.logging.LogFileWriter;

/**
 * Cleans up the execution of a stage by releasing resources.
 * 
 * @author Kevin Seidler
 * 
 */
public final class CleanUpAction implements NoResultAction {

	private final LogFileWriter logFileWriter;
	private final WsdlInterface wsdlInterface;

	public CleanUpAction(LogFileWriter logFileWriter, WsdlInterface wsdlInterface) {
		this.logFileWriter = logFileWriter;
		this.wsdlInterface = wsdlInterface;
	}

	@Override
	public void execute() {
		cleanUp(logFileWriter, wsdlInterface);
	}

	private void cleanUp(LogFileWriter logFileWriter, WsdlInterface wsdlInterface) {

		if (wsdlInterface != null) {
			wsdlInterface.getProject().release();
		}

		logFileWriter.close();
		// soapUI maintains a connection pool, they have to be closed for the failure detector to work correctly
		HttpClientSupport.getHttpClient().getHttpConnectionManager().closeIdleConnections(0);
	}
}