package eu.heliovo.monitoring.service;

import java.util.*;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;

import eu.heliovo.monitoring.exporter.ServiceStatusDetailsExporter;
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
	private final ServiceStatusDetailsExporter serviceStatusDetailsExporter;

	private int currentServicesHashCode = 0;

	// for manual service definition, please use "staticServiceLoader" as qualifier and define services in Services.java
	@Autowired
	public MonitoringServiceImpl(PingStage pingStage, MethodCallStage methodCallStage,
			TestingStage testingStage,
			@Qualifier("ivoaRegistryServiceLoader") ServiceLoader serviceLoader,
			ServiceStatusDetailsExporter serviceStatusDetailsExporter,
			List<ServiceUpdateListener> serviceUpdateListeners) { // Spring automatically injects all components
																	// implementing the ServiceUpdateListener interface

		this.pingStage = pingStage;
		this.methodCallStage = methodCallStage;
		this.testingStage = testingStage;
		this.serviceLoader = serviceLoader;
		this.serviceStatusDetailsExporter = serviceStatusDetailsExporter;
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
		if (newServices.hashCode() != currentServicesHashCode) {
			updateServiceUpdateListeners(newServices);
			currentServicesHashCode = newServices.hashCode();
		}
	}

	private void updateServiceUpdateListeners(Set<Service> newServices) {
		for (ServiceUpdateListener listener : serviceUpdateListeners) {
			listener.updateServices(newServices);
		}
	}

	@Scheduled(cron = "${pingStage.updateInterval.cronValue}")
	protected void updatePingStatusAndExport() {
		updateAndExportStatus(pingStage);
	}

	@Scheduled(cron = "${methodCallStage.updateInterval.cronValue}")
	protected void updateMethodCallStatusAndExport() {
		updateAndExportStatus(methodCallStage);
	}

	@Scheduled(cron = "${testingStage.updateInterval.cronValue}")
	protected void updateTestingStatusAndExport() {
		updateAndExportStatus(testingStage);
	}

	private void updateAndExportStatus(MonitoringStage stage) {

		stage.updateStatus();

		List<ServiceStatusDetails> status = stage.getServicesStatus();
		serviceStatusDetailsExporter.exportServiceStatusDetails(status);

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