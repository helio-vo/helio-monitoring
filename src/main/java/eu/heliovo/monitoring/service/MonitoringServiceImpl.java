package eu.heliovo.monitoring.service;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.*;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;

import eu.heliovo.monitoring.exporter.StatusDetailsExporter;
import eu.heliovo.monitoring.listener.ServiceUpdateListener;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.serviceloader.ServiceLoader;
import eu.heliovo.monitoring.stage.*;

/**
 * The MonitoringService instatiated as web service. Does only provide getStatus with services, their status (up or
 * down) and response time if up.
 * 
 * @author Kevin Seidler
 * 
 */
@org.springframework.stereotype.Service
public final class MonitoringServiceImpl implements MonitoringService {

	private final Logger logger = Logger.getLogger(this.getClass());

	private final PingStage pingStage;
	private final MethodCallStage methodCallStage;
	private final TestingStage testingStage;

	private final ServiceLoader serviceLoader;
	private final List<ServiceUpdateListener> serviceUpdateListeners;
	private final StatusDetailsExporter statusDetailsExporter;

	private int currentServicesHashCode = 0;

	// for manual service definition, please use "staticServiceLoader" as qualifier and define services in Services.java
	// Spring automatically injects all components implementing the ServiceUpdateListener interface
	@Autowired
	public MonitoringServiceImpl(PingStage pingStage, MethodCallStage methodCallStage, TestingStage testingStage,
			@Qualifier("ivoaRegistryServiceLoader") ServiceLoader serviceLoader,
			StatusDetailsExporter statusDetailsExporter, List<ServiceUpdateListener> serviceUpdateListeners) {

		this.pingStage = pingStage;
		this.methodCallStage = methodCallStage;
		this.testingStage = testingStage;
		this.serviceLoader = serviceLoader;
		this.statusDetailsExporter = statusDetailsExporter;
		this.serviceUpdateListeners = Collections.unmodifiableList(serviceUpdateListeners);

		this.updateServices();
		// TODO dont update here when services are fetched from the registry, deployment/starting of the service could
		// take too much time, or use a start delay, but what to do without services? persist the last list somewhere?
	}

	/**
	 * This method is called regularly from Spring to update the available services using the Scheduled annotation.
	 */
	@Scheduled(cron = "${registry.updateInterval.cronValue}")
	protected void updateServices() {

		Set<Service> newServices = serviceLoader.loadServices();
		updateServiceUpdateListenersOnChange(newServices);
	}

	private void updateServiceUpdateListenersOnChange(Set<Service> newServices) {
		if (haveServicesChanged(newServices)) {
			updateServiceUpdateListeners(newServices);
			currentServicesHashCode = newServices.hashCode();
		}
	}

	private boolean haveServicesChanged(Set<Service> newServices) {
		return !isEmpty(newServices) && newServices.hashCode() != currentServicesHashCode;
	}

	private void updateServiceUpdateListeners(Set<Service> newServices) {
		for (ServiceUpdateListener listener : serviceUpdateListeners) {
			listener.updateServices(newServices);
		}
	}

	@Scheduled(cron = "${pingStage.updateInterval.cronValue}")
	protected void updatePingStatusAndExport() {

		pingStage.updateStatus();
		exportHostStatus(pingStage);
		exportServiceStatus(pingStage);
	}

	private void exportHostStatus(MonitoringStage stage) {

		Map<String, ServiceStatusDetails> mappedHostStatusDetails = new HashMap<String, ServiceStatusDetails>();
		for (ServiceStatusDetails serviceStatusDetails : stage.getServicesStatus()) {
			mappedHostStatusDetails.put(serviceStatusDetails.getUrl().getHost(), serviceStatusDetails);
		}

		List<ServiceStatusDetails> hostStatusDetails;
		hostStatusDetails = new ArrayList<ServiceStatusDetails>(mappedHostStatusDetails.values());

		statusDetailsExporter.exportHostStatusDetails(hostStatusDetails);
	}

	@Scheduled(cron = "${methodCallStage.updateInterval.cronValue}")
	protected void updateMethodCallStatusAndExport() {
		methodCallStage.updateStatus();
		exportServiceStatus(methodCallStage);
	}

	@Scheduled(cron = "${testingStage.updateInterval.cronValue}")
	protected void updateTestingStatusAndExport() {
		testingStage.updateStatus();
		exportServiceStatus(testingStage);
	}

	private void exportServiceStatus(MonitoringStage stage) {

		List<ServiceStatusDetails> status = stage.getServicesStatus();
		statusDetailsExporter.exportServiceStatusDetails(status);
		logExport(status);
	}

	private void logExport(List<ServiceStatusDetails> result) {
		if (logger.isDebugEnabled()) {
			logger.debug("refreshed stage's cache");
			for (ServiceStatusDetails serviceStatusDetails : result) {
				logger.debug("service: " + serviceStatusDetails.getName() + " status: "
						+ serviceStatusDetails.getStatus().toString() + " response time: "
						+ serviceStatusDetails.getResponseTimeInMillis() + " ms");
			}
		}
	}

	@Override
	public List<ServiceStatusDetails> getPingStatus() {
		return pingStage.getServicesStatus();
	}

	@Override
	public List<ServiceStatusDetails> getMethodCallStatus() {
		return methodCallStage.getServicesStatus();
	}

	@Override
	public List<ServiceStatusDetails> getTestingStatus() {
		return testingStage.getServicesStatus();
	}
}