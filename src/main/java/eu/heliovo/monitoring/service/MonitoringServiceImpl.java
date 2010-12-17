package eu.heliovo.monitoring.service;

import java.util.*;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Assert;

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

	@Autowired
	public MonitoringServiceImpl(
			PingStage pingStage,
			MethodCallStage methodCallStage,
			TestingStage testingStage,
			@Qualifier("staticServiceLoader") ServiceLoader serviceLoader,
			@Qualifier("compositeServiceStatusDetailsExporter") ServiceStatusDetailsExporter serviceStatusDetailsExporter,
			List<ServiceUpdateListener> serviceUpdateListeners) { // Spring automatically injects all components of
																	// those listeners
		this.pingStage = pingStage;
		this.methodCallStage = methodCallStage;
		this.testingStage = testingStage;
		this.serviceLoader = serviceLoader;
		this.serviceStatusDetailsExporter = serviceStatusDetailsExporter;
		this.serviceUpdateListeners = Collections.unmodifiableList(serviceUpdateListeners);

		this.validateState();
		this.updateServices();
	}

	private void validateState() {

		Assert.notNull(pingStage, "the pingStage must not be null");
		Assert.notNull(methodCallStage, "the methodCallStage must not be null");
		Assert.notNull(testingStage, "the testingStage must not be null");
	}

	// TODO extract update methods to a new class
	/**
	 * This method is called regularly from Spring to update the available services using the Scheduled annotation.
	 */
	@Scheduled(cron = "${registry.updateInterval.cronValue}")
	protected void updateServices() {

		// TODO automatic nagios config generation needed, static service definition used till implemented
		List<Service> newServices = serviceLoader.loadServices();
		updateServiceUpdateListeners(newServices);
	}

	private void updateServiceUpdateListeners(List<Service> newServices) {
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