package eu.heliovo.monitoring.action;

import java.util.concurrent.*;

import com.eviware.soapui.impl.wsdl.*;
import com.eviware.soapui.impl.wsdl.submit.transports.http.WsdlResponse;
import com.eviware.soapui.model.iface.Request.SubmitException;

import eu.heliovo.monitoring.logging.LogFileWriter;

/**
 * Submits a SOAP request and waits for the response within a certain timeout.
 * 
 * @author Kevin Seidler
 * 
 */
public class SubmitRequestAction implements ResultAction<WsdlResponse> {

	private static final boolean SUBMIT_ASYNC = false;
	private static final int RESPONSE_TIMEOUT_IN_SECONDS = 10;

	private final WsdlRequest request;
	private final LogFileWriter logFileWriter;
	private final ExecutorService executor;

	public SubmitRequestAction(WsdlRequest request, LogFileWriter logFileWriter, ExecutorService executor) {
		this.request = request;
		this.logFileWriter = logFileWriter;
		this.executor = executor;
	}

	@Override
	public WsdlResponse getResult() throws Exception {
		return submitRequest(request, logFileWriter);
	}

	private WsdlResponse submitRequest(final WsdlRequest request, LogFileWriter logFileWriter)
			throws ExecutionException, InterruptedException, SubmitException {

		logFileWriter.write("Sending request");

		Future<WsdlResponse> future = executor.submit(new Callable<WsdlResponse>() {
			@Override
			public WsdlResponse call() throws SubmitException {

				WsdlSubmitContext wsdlSubmitContext = new WsdlSubmitContext(request.getModelItem());
				WsdlSubmit<WsdlRequest> submit = request.submit(wsdlSubmitContext, SUBMIT_ASYNC);

				return (WsdlResponse) submit.getResponse();
			}
		});

		logFileWriter.write("Waiting for the response");

		try {
			// TODO automatically determine timeout
			return future.get(RESPONSE_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);

		} catch (TimeoutException e) {
			throw new IllegalStateException("Waiting for response timed out (timeout: " + RESPONSE_TIMEOUT_IN_SECONDS
					+ " s)");
		} finally {
			future.cancel(true);
		}
	}
}