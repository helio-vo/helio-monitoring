package eu.heliovo.monitoring.action;

import java.io.IOException;
import java.util.concurrent.*;

import org.apache.xmlbeans.XmlException;

import com.eviware.soapui.impl.WsdlInterfaceFactory;
import com.eviware.soapui.impl.wsdl.*;
import com.eviware.soapui.support.SoapUIException;

import eu.heliovo.monitoring.logging.LogFileWriter;

/**
 * This Action loads a WSDL file from a specifies URL and parses it for further use.
 * 
 * @author Kevin Seidler
 * 
 */
public final class ImportWsdlAction implements Action<WsdlInterface> {

	private static final int IMPORT_WSDL_TIMEOUT_IN_SECONDS = 20;
	private static final int FIRST_WSDL_INTERFACE = 0;

	private final LogFileWriter logFileWriter;
	private final String wsdlUrl;
	private final ExecutorService executor;

	public ImportWsdlAction(LogFileWriter logFileWriter, String wsdlUrl, ExecutorService executor) {
		this.logFileWriter = logFileWriter;
		this.wsdlUrl = wsdlUrl;
		this.executor = executor;
	}

	@Override
	public WsdlInterface getResult() throws XmlException, IOException, SoapUIException, InterruptedException,
			ExecutionException {

		final WsdlProject project = new WsdlProject();

		logFileWriter.write("Importing WSDL file " + wsdlUrl);

		Future<WsdlInterface> future = executor.submit(new Callable<WsdlInterface>() {
			@Override
			public WsdlInterface call() throws SoapUIException {

				WsdlInterface[] wsdlInterfaces = WsdlInterfaceFactory.importWsdl(project, wsdlUrl, true);
				return wsdlInterfaces[FIRST_WSDL_INTERFACE];
			}
		});

		try {
			// TODO automatically determine timeout with help of the failure detector, parsing (size of wsdl file)
			// should be excluded, to do correct timeout calculation
			WsdlInterface wsdlInterface = future.get(IMPORT_WSDL_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
			logFileWriter.write("Importing finished");

			return wsdlInterface;

		} catch (TimeoutException e) {
			project.release();
			throw new IllegalStateException("Importing WSDL file timed out (timeout: " + IMPORT_WSDL_TIMEOUT_IN_SECONDS + " s)");
		} finally {
			future.cancel(true);
		}
	}
}