package eu.heliovo.monitoring.daemon;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.heliovo.monitoring.model.ServiceStatusDetails;
import eu.heliovo.monitoring.model.Status;

/**
 * TODO document me!<br/>
 * TODO generate nagios service configs (e.g. with XSLT) and restart nagios daemon (see
 * http://nagios.sourceforge.net/docs/1_0/extcommands.html Command: RESTART_PROGRAM)
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class MonitoringDaemon implements RemotingMonitoringDaemon {

	private static final int TO_SECONDS_DIVISOR = 1000;

	public static final String FILE_ENCODING = "UTF-8";

	private final File nagiosExternalCommandFile;
	private final Logger logger = Logger.getLogger(getClass());
	private final boolean forceNagiosExternalCommandFileCreation;

	@Autowired
	public MonitoringDaemon(@Value("${nagiosExternalCommandFilePath}") String nagiosExternalCommandFile,
			@Value("${forceNagiosExternalCommandFileCreation}") boolean forceNagiosExternalCommandFileCreation) {

		this.nagiosExternalCommandFile = new File(nagiosExternalCommandFile);
		this.forceNagiosExternalCommandFileCreation = forceNagiosExternalCommandFileCreation;

		this.validateCommandFileAccess();
	}

	private void validateCommandFileAccess() {

		if (nagiosExternalCommandFile != null && !nagiosExternalCommandFile.exists()) {
			if (forceNagiosExternalCommandFileCreation) {
				logger.info("nagiosExternalCommandFile does not exist, but will be created");
				try {
					nagiosExternalCommandFile.createNewFile();
				} catch (IOException e) {
					throw new IllegalStateException("nagiosExternalCommandFile could not be created!", e);
				}
			} else {
				logger.warn("nagiosExternalCommandFile does not exist and is not allowed to be created!");
			}
		}

		if (nagiosExternalCommandFile == null) {
			throw new IllegalStateException("nagiosExternalCommandFile must not be null!");
		} else if (nagiosExternalCommandFile.exists() && !nagiosExternalCommandFile.canWrite()) {
			throw new IllegalStateException("nagiosExternalCommandFile cannot be written!");
		}
	}

	public void writeToNagiosExternalCommandFile(long timeInSeconds, NagiosCommand command, String hostName,
			String serviceName, NagiosServiceStatus status, String statusMessage) {

		if (nagiosExternalCommandFile.exists() || forceNagiosExternalCommandFileCreation) {
			String lineToWrite = buildLineToWrite(timeInSeconds, command, hostName, serviceName, status, statusMessage);
			try {

				FileWriter fileWriter = new FileWriter(nagiosExternalCommandFile, true);
				try {
					fileWriter.write(lineToWrite);
				} finally {
					fileWriter.close();
				}

			} catch (IOException e) {
				throw new IllegalStateException("data could not be written!", e);
			}
		}
	}

	public void writeToNagiosExternalCommandFile(NagiosCommand command, String hostName, String serviceName,
			NagiosServiceStatus status, String statusMessage) {

		long timeInSeconds = System.currentTimeMillis() / TO_SECONDS_DIVISOR;
		this.writeToNagiosExternalCommandFile(timeInSeconds, command, hostName, serviceName, status, statusMessage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.heliovo.monitoring.daemon.RemotingMonitoringDaemon#writeServiceStatusToNagios(java.util.List)
	 */
	public void writeServiceStatusToNagios(List<ServiceStatusDetails> serviceStatusDetails) {

		for (ServiceStatusDetails actualServiceStatusDetails : serviceStatusDetails) {

			NagiosCommand command = NagiosCommand.PROCESS_SERVICE_CHECK_RESULT;
			String hostName = actualServiceStatusDetails.getUrl().getHost();
			String serviceName = actualServiceStatusDetails.getId();

			NagiosServiceStatus nagiosStatus;
			Status serviceStatus = actualServiceStatusDetails.getStatus();
			if (Status.OK.equals(serviceStatus)) {
				nagiosStatus = NagiosServiceStatus.OK;
			} else if (Status.CRITICAL.equals(serviceStatus)) {
				nagiosStatus = NagiosServiceStatus.CRITICAL;
			} else if (Status.WARNING.equals(serviceStatus)) {
				nagiosStatus = NagiosServiceStatus.WARNING;
			} else if (Status.UNKNOWN.equals(serviceStatus)) {
				nagiosStatus = NagiosServiceStatus.UNKNOWN;
			} else {
				throw new IllegalStateException("a state must be given!");
			}

			writeToNagiosExternalCommandFile(command, hostName, serviceName, nagiosStatus,
					actualServiceStatusDetails.getMessage());
		}
	}

	private String buildLineToWrite(long timeInSeconds, NagiosCommand command, String hostName, String serviceName,
			NagiosServiceStatus status, String statusMessage) {

		StringBuffer buffer = new StringBuffer("[");
		buffer.append(timeInSeconds);
		buffer.append("]");
		buffer.append(" ");
		buffer.append(command.name());
		buffer.append(";");
		buffer.append(hostName);
		buffer.append(";");
		buffer.append(serviceName);
		buffer.append(";");
		buffer.append(status.ordinal());
		buffer.append(";");
		buffer.append(statusMessage);
		buffer.append("\n");

		return buffer.toString();
	}
}
