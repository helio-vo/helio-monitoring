package eu.heliovo.monitoring.action;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.xmlbeans.XmlException;

import com.eviware.soapui.impl.WsdlInterfaceFactory;
import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.support.SoapUIException;

import eu.heliovo.monitoring.logging.LogFileWriter;

/**
 * This Action loads a WSDL file from a specifies URL and parses it for further use.
 * 
 * @author Kevin Seidler
 * 
 */
public final class ImportWsdlAction implements ResultAction<WsdlInterface> {

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
				if (wsdlInterfaces == null || wsdlInterfaces.length == 0) {
				    throw new SoapUIException("Unable to load WsdlInterface for URL " + wsdlUrl);
				} 
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
	
	public static void main(String[] args) throws Exception {
	    ExecutorService executor = null;
        LogFileWriter logger = new LogFileWriter() {
            
            @Override
            public void write(Exception e) {
                e.printStackTrace();
            }
            
            @Override
            public void write(String text) {
                System.err.println(text);
            }
            
            @Override
            public String getFileName() {
                return "test";
            }
            
            @Override
            public void close() {
            }
        };
        ImportWsdlAction action = new ImportWsdlAction(logger , "http://festung1.oats.inaf.it:8080/helio-hec/HelioService?wsdl", executor);
        WsdlInterface res = action.getResult();
        System.out.println(res);
        res = null;
        
    }
}