package eu.heliovo.monitoring.action;

import static eu.heliovo.monitoring.model.ModelFactory.newStatusDetails;

import java.util.List;

import eu.heliovo.monitoring.logging.*;
import eu.heliovo.monitoring.model.*;

/**
 * The Action handles errors occuring while the execution of a stage.
 * 
 * @author Kevin Seidler
 * 
 */
public final class HandleErrorAction implements NoResultAction {

	private final Exception exception;
	private final LogFileWriter logFileWriter;
	private final String serviceName;
	private final Service service;
	private final List<StatusDetails<Service>> servicesStatus;
	private final String logFilesUrl;

	public HandleErrorAction(Exception exception, LogFileWriter logFileWriter, String serviceName, Service service,
			List<StatusDetails<Service>> servicesStatus, String logFilesUrl) {

		this.exception = exception;
		this.logFileWriter = logFileWriter;
		this.serviceName = serviceName;
		this.service = service;
		this.servicesStatus = servicesStatus;
		this.logFilesUrl = logFilesUrl;
	}

	@Override
	public void execute() {
		handleError(exception, logFileWriter, serviceName, service, servicesStatus);
	}

	private void handleError(Exception exception, LogFileWriter logFileWriter, String serviceName, Service service,
			List<StatusDetails<Service>> newCache) {

		String message = "An error occured: " + exception.getMessage()
				+ LoggingHelper.getLogFileText(logFileWriter, logFilesUrl);

		newCache.add(newStatusDetails(service, serviceName, service.getUrl(), Status.CRITICAL, 0, message));

		logFileWriter.write(exception);
	}
}