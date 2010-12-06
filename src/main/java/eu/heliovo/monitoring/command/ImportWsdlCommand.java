package eu.heliovo.monitoring.command;

import java.io.IOException;
import java.util.concurrent.*;

import org.apache.xmlbeans.XmlException;

import com.eviware.soapui.impl.WsdlInterfaceFactory;
import com.eviware.soapui.impl.wsdl.*;
import com.eviware.soapui.support.SoapUIException;

import eu.heliovo.monitoring.logging.LogFileWriter;

public final class ImportWsdlCommand {

	private static final int IMPORT_WSDL_TIMEOUT = 10;
	private static final int FIRST_WSDL_INTERFACE = 0;

	private final LogFileWriter logFileWriter;
	private final String wsdlUrl;
	private final ExecutorService executor;

	public ImportWsdlCommand(LogFileWriter logFileWriter, String wsdlUrl, ExecutorService executor) {
		this.logFileWriter = logFileWriter;
		this.wsdlUrl = wsdlUrl;
		this.executor = executor;
	}

	public WsdlInterface execute() throws XmlException, IOException, SoapUIException, InterruptedException,
			ExecutionException {

		final WsdlProject project = new WsdlProject();

		logFileWriter.write("Importing WSDL file " + wsdlUrl);

		Future<WsdlInterface> future = executor.submit(new Callable<WsdlInterface>() {
			@Override
			public WsdlInterface call() throws SoapUIException {
				// TODO parsing (size of wsdl file) should be excluded, to do correct timeout calculation
				WsdlInterface[] wsdlInterfaces = WsdlInterfaceFactory.importWsdl(project, wsdlUrl, true);
				return wsdlInterfaces[FIRST_WSDL_INTERFACE];
			}
		});

		try {
			// TODO automatically determine timeout
			WsdlInterface wsdlInterface = future.get(IMPORT_WSDL_TIMEOUT, TimeUnit.SECONDS);

			logFileWriter.write("Importing finished");

			return wsdlInterface;

		} catch (TimeoutException e) {
			project.release();
			throw new IllegalStateException("Importing WSDL file timed out (timeout: " + IMPORT_WSDL_TIMEOUT + " s)");
		} finally {
			future.cancel(true);
		}
	}
}