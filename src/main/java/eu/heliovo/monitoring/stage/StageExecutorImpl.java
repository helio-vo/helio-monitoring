package eu.heliovo.monitoring.stage;

import static eu.heliovo.monitoring.model.ModelFactory.newStatusDetails;
import static eu.heliovo.monitoring.model.Status.CRITICAL;

import java.net.URL;
import java.util.*;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import eu.heliovo.monitoring.exporter.StatusDetailsExporter;
import eu.heliovo.monitoring.model.*;

/**
 * This executor runs the 3 stages with the following behavior:<br/>
 * <br/>
 * 
 * - run Ping; result: for every host, that is up, go on with:<br/>
 * - run Method call, result: for every service, that is up, go on with:<br/>
 * - run Testing<br/>
 * - return and export a common result of all stages
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class StageExecutorImpl implements StageExecutor {

	private static final int START_DELAY_IN_MILLIS = 500;
	private static final int SLEEPING_TIME_IN_MILLIS = 100;

	private final Logger logger = Logger.getLogger(this.getClass());

	private final PingStage pingStage;
	private final MethodCallStage methodCallStage;
	private final TestingStage testingStage;
	private final StatusDetailsExporter statusDetailsExporter;
	private final TaskScheduler scheduler;

	private Set<Host> hosts = Collections.emptySet();

	private List<StatusDetails<Service>> latestStatus = Collections.emptyList();

	@Autowired
	protected StageExecutorImpl(PingStage pingStage, MethodCallStage methodCallStage, TestingStage testingStage,
			StatusDetailsExporter statusDetailsExporter, TaskScheduler scheduler) {

		this.pingStage = pingStage;
		this.methodCallStage = methodCallStage;
		this.testingStage = testingStage;
		this.statusDetailsExporter = statusDetailsExporter;
		this.scheduler = scheduler;
	}

	@Override
	public void doContinousExecution() {

		Calendar startTime = calculateStartTime();

		scheduler.schedule(new Runnable() {
			@Override
			public void run() {

				while (true) {
					try {
						execute();
						Thread.sleep(SLEEPING_TIME_IN_MILLIS); // to let other threads get to work

					} catch (Throwable t) {
						System.out.println("StageExecutor: Exception in continuous Execution");
						t.printStackTrace();
						logger.warn(t.getMessage(), t);
					}
				}
			}
		}, startTime.getTime()); // a start delay is used to be sure that the rest of the system is initialized
	}

	private Calendar calculateStartTime() {
		Calendar startTime = Calendar.getInstance();
		startTime.setTimeInMillis(System.currentTimeMillis() + START_DELAY_IN_MILLIS);
		return startTime;
	}

	@Override
	public synchronized List<StatusDetails<Service>> getStatus() {
		return latestStatus;
	}

	@Override
	public synchronized void updateHosts(Set<Host> newHosts) {
		this.hosts = newHosts;
	}

	@Override
	public void execute() {

		List<StatusDetails<Service>> latestStatus = new ArrayList<StatusDetails<Service>>();

		// the Method getStatus executes a stage and returns its results
		List<StatusDetails<Host>> pingStatus = pingStage.getStatus(getHosts()); // List of Hosts OK or CRITICAL

		Set<Service> servicesWithHostsOk = partServicesForMethodCall(latestStatus, pingStatus);
		List<StatusDetails<Service>> methodCallStatus = methodCallStage.getStatus(servicesWithHostsOk);

		Set<Service> servicesOk = partServicesForTesting(latestStatus, methodCallStatus);
		List<StatusDetails<Service>> testingStatus = testingStage.getStatus(servicesOk);

		latestStatus.addAll(testingStatus);
		setLatestStatus(latestStatus);

		exportStatusDetails(latestStatus, pingStatus);
	}

	private synchronized Set<Host> getHosts() {
		return hosts;
	}

	private synchronized void setLatestStatus(List<StatusDetails<Service>> latestStatus) {
		this.latestStatus = latestStatus;
	}

	private void exportStatusDetails(List<StatusDetails<Service>> latestStatus, List<StatusDetails<Host>> pingStatus) {
		statusDetailsExporter.exportHostStatusDetails(pingStatus);
		statusDetailsExporter.exportServiceStatusDetails(latestStatus);
		logExport(latestStatus);
	}

	private Set<Service> partServicesForTesting(List<StatusDetails<Service>> latestStatus,
			List<StatusDetails<Service>> methodCallStatus) {

		Set<Service> servicesOk = new HashSet<Service>();
		for (StatusDetails<Service> serviceStatusDetails : methodCallStatus) {
			if (isOk(serviceStatusDetails)) {
				servicesOk.add(serviceStatusDetails.getMonitoredEntity());
			} else {
				latestStatus.add(serviceStatusDetails);
			}
		}
		return servicesOk;
	}

	private Set<Service> partServicesForMethodCall(List<StatusDetails<Service>> latestStatus,
			List<StatusDetails<Host>> pingStatus) {

		Set<Service> servicesWithHostsOk = new HashSet<Service>();
		for (StatusDetails<Host> hostStatusDetails : pingStatus) {
			if (isOk(hostStatusDetails)) {
				addAllServices(servicesWithHostsOk, hostStatusDetails);
			} else {
				addServicesToLatestStatus(latestStatus, hostStatusDetails);
			}
		}
		return servicesWithHostsOk;
	}

	private void addServicesToLatestStatus(List<StatusDetails<Service>> latestStatus,
			StatusDetails<Host> hostStatusDetails) {

		Host host = hostStatusDetails.getMonitoredEntity();
		for (Service service : host.getServices()) {

			String serviceName = service.getName();
			URL serviceUrl = service.getUrl();
			String message = hostStatusDetails.getMessage();

			latestStatus.add(newStatusDetails(service, serviceName, serviceUrl, CRITICAL, Long.MAX_VALUE, message));
		}
	}

	private void addAllServices(Set<Service> servicesWithHostsOk, StatusDetails<Host> hostStatusDetails) {
		Host host = hostStatusDetails.getMonitoredEntity();
		servicesWithHostsOk.addAll(host.getServices());
	}

	private boolean isOk(StatusDetails<?> hostStatusDetails) {
		return Status.OK.equals(hostStatusDetails.getStatus());
	}

	private void logExport(List<StatusDetails<Service>> result) {
		if (logger.isDebugEnabled()) {
			logger.debug("refreshed stage's cache");
			for (StatusDetails<Service> serviceStatusDetails : result) {
				logger.debug("service: " + serviceStatusDetails.getName() + " status: "
						+ serviceStatusDetails.getStatus().toString() + " response time: "
						+ serviceStatusDetails.getResponseTimeInMillis() + " ms");
			}
		}
	}
}