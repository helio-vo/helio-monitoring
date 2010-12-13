package eu.heliovo.monitoring.stage;

import java.util.List;

import org.springframework.stereotype.Component;

import eu.heliovo.monitoring.failuredetector.ServiceFailureDetector;
import eu.heliovo.monitoring.model.*;

/**
 * The ping stage uses a failure detector which continuously detects failures of the services hosts. The WSDL file url
 * of the services is used to connect to the service host and measure response time continuously. By convention the WSDL
 * file is on the same host as the service. Therefore it is not needed to search the WSDL file to find the service or
 * host address.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class PingStage implements MonitoringStage {

	protected static final String SERVICE_NAME_SUFFIX = " -ping-";

	private final ServiceFailureDetector failureDetector;

	public PingStage(ServiceFailureDetector failureDetector) {
		this.failureDetector = failureDetector;
	}

	@Override
	public void updateStatus() {
		// no manual update needed, the failure detector continuously detects failures and returns latest results
	}

	@Override
	public void setServices(List<Service> services) {
		failureDetector.updateServices(services);
	}

	@Override
	public List<ServiceStatusDetails> getServicesStatus() {
		return failureDetector.getServicesStatus(SERVICE_NAME_SUFFIX);
	}
}