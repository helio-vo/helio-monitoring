package eu.heliovo.monitoring.stage;

import static eu.heliovo.monitoring.model.ModelFactory.newStatusDetails;
import static eu.heliovo.monitoring.model.Status.CRITICAL;
import static eu.heliovo.monitoring.model.Status.OK;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.heliovo.monitoring.failuredetector.FailureDetector;
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
public final class PingStageImpl implements PingStage {

	private final FailureDetector failureDetector;

	@Autowired
	protected PingStageImpl(FailureDetector failureDetector) {
		this.failureDetector = failureDetector;
	}

	@Override
	public List<StatusDetails<Host>> getStatus(Set<Host> hosts) {

		List<StatusDetails<Host>> hostStatus = new ArrayList<StatusDetails<Host>>();

		for (Host host : hosts) {

			boolean hostIsAlive = failureDetector.isAlive(host);
			long responseTimeInMillis = failureDetector.getResponseTimeInMillis(host);

			Status status = hostIsAlive ? OK : CRITICAL;
			String message;
			StatusDetails<Host> details;

			if (hostIsAlive) {

				message = "Service host " + host + "  is reachable, response time = " + responseTimeInMillis + " ms";

			} else {

				responseTimeInMillis = Long.MAX_VALUE; // to be able to compare services by response time
				message = "Service is unreachable";
			}

			details = newStatusDetails(host, host.getName(), host.getUrl(), status, responseTimeInMillis, message);

			hostStatus.add(details);
		}

		return hostStatus;
	}
}