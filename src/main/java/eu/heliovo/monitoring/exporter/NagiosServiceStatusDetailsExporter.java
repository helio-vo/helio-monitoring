package eu.heliovo.monitoring.exporter;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.heliovo.monitoring.model.*;

/**
 * Exports information of {@link ServiceStatusDetails} to the {@link NagiosCommandWriter} to send it to Nagios for
 * representation in its web interface.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class NagiosServiceStatusDetailsExporter implements StatusDetailsExporter {

	private final NagiosCommandWriter nagiosCommandWriter;

	@Autowired
	public NagiosServiceStatusDetailsExporter(NagiosCommandWriter nagiosCommandWriter) {
		this.nagiosCommandWriter = nagiosCommandWriter;
	}

	@Override
	public void exportHostStatusDetails(List<ServiceStatusDetails> serviceStatusDetails) {

		for (ServiceStatusDetails currentStatusDetails : serviceStatusDetails) {

			NagiosCommand command = NagiosCommand.PROCESS_HOST_CHECK_RESULT;
			String hostName = currentStatusDetails.getUrl().getHost();

			NagiosServiceStatus nagiosStatus = getNagiosServiceStatus(currentStatusDetails);

			List<String> commandArguments = new ArrayList<String>();
			commandArguments.add(hostName);
			commandArguments.add(String.valueOf(nagiosStatus.ordinal()));
			commandArguments.add(buildHostStatusMessage(currentStatusDetails, nagiosStatus));

			nagiosCommandWriter.write(command, commandArguments);
		}
	}

	private String buildHostStatusMessage(ServiceStatusDetails currentStatusDetails, NagiosServiceStatus nagiosStatus) {

		long responseTime = currentStatusDetails.getResponseTimeInMillis();
		String hostReachableMessage = "Host is reachable, response time = " + responseTime + " ms";

		return nagiosStatus.equals(NagiosServiceStatus.OK) ? hostReachableMessage : "Host not reachable";
	}

	@Override
	public void exportServiceStatusDetails(List<ServiceStatusDetails> serviceStatusDetails) {

		for (ServiceStatusDetails currentStatusDetails : serviceStatusDetails) {

			NagiosCommand command = NagiosCommand.PROCESS_SERVICE_CHECK_RESULT;
			String hostName = currentStatusDetails.getUrl().getHost();
			String serviceName = currentStatusDetails.getName();

			NagiosServiceStatus nagiosStatus = getNagiosServiceStatus(currentStatusDetails);

			List<String> commandArguments;
			commandArguments = assembleCommandArguments(currentStatusDetails, hostName, serviceName, nagiosStatus);

			nagiosCommandWriter.write(command, commandArguments);
		}
	}

	private List<String> assembleCommandArguments(ServiceStatusDetails actualServiceStatusDetails, String hostName,
			String serviceName, NagiosServiceStatus nagiosStatus) {

		List<String> commandArguments = new ArrayList<String>();
		commandArguments.add(hostName);
		commandArguments.add(serviceName);
		commandArguments.add(String.valueOf(nagiosStatus.ordinal()));
		commandArguments.add(actualServiceStatusDetails.getMessage());
		return commandArguments;
	}

	protected NagiosServiceStatus getNagiosServiceStatus(ServiceStatusDetails actualServiceStatusDetails) {

		NagiosServiceStatus nagiosStatus;
		ServiceStatus serviceStatus = actualServiceStatusDetails.getStatus();

		if (ServiceStatus.OK.equals(serviceStatus)) {
			nagiosStatus = NagiosServiceStatus.OK;
		} else if (ServiceStatus.CRITICAL.equals(serviceStatus)) {
			nagiosStatus = NagiosServiceStatus.CRITICAL;
		} else if (ServiceStatus.WARNING.equals(serviceStatus)) {
			nagiosStatus = NagiosServiceStatus.WARNING;
		} else if (ServiceStatus.UNKNOWN.equals(serviceStatus)) {
			nagiosStatus = NagiosServiceStatus.UNKNOWN;
		} else {
			throw new IllegalStateException("a state must be given!");
		}
		return nagiosStatus;
	}
}