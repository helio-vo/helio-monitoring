package eu.heliovo.monitoring.command;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import com.eviware.soapui.impl.WsdlInterfaceFactory;
import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.support.SoapUIException;

import eu.heliovo.monitoring.logging.LogFileWriter;

public final class ImportWsdlCommand {

	private static final int IMPORT_WSDL_TIMEOUT = 10;
	private static final int FIRST_WSDL_INTERFACE = 0;

	private final Logger logger = Logger.getLogger(this.getClass());

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

		logger.debug("Importing WSDL file " + wsdlUrl);
		logFileWriter.writeToLogFile("Importing WSDL file " + wsdlUrl);

		Future<WsdlInterface> future = executor.submit(new Callable<WsdlInterface>() {
			public WsdlInterface call() throws SoapUIException {
				WsdlInterface[] wsdlInterfaces = WsdlInterfaceFactory.importWsdl(project, wsdlUrl, true);
				return wsdlInterfaces[FIRST_WSDL_INTERFACE];
			}
		});

		try {
			// TODO automatically determine timeout
			WsdlInterface wsdlInterface = future.get(IMPORT_WSDL_TIMEOUT, TimeUnit.SECONDS);

			logger.debug("Importing finished");
			logFileWriter.writeToLogFile("Importing finished");

			return wsdlInterface;

		} catch (TimeoutException e) {
			project.release();
			throw new IllegalStateException("Importing WSDL file timed out (timeout: " + IMPORT_WSDL_TIMEOUT + " s)");
		}
	}
}