package eu.heliovo.monitoring.service;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Assert;

import eu.heliovo.monitoring.component.*;
import eu.heliovo.monitoring.exporter.ServiceStatusDetailsExporter;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.serviceloader.ServiceLoader;

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

	private final PingComponent pingComponent;
	private final MethodCallComponent methodCallComponent;
	private final TestingComponent testingComponent;

	private final ServiceLoader serviceLoader;

	private final ServiceStatusDetailsExporter serviceStatusDetailsExporter;

	@Autowired
	public MonitoringServiceImpl(
			PingComponent pingComponent,
			MethodCallComponent methodCallComponent,
			TestingComponent testingComponent,
			@Qualifier("staticServiceLoader") ServiceLoader serviceLoader,
			@Qualifier("compositeServiceStatusDetailsExporter") ServiceStatusDetailsExporter serviceStatusDetailsExporter) {

		this.pingComponent = pingComponent;
		this.methodCallComponent = methodCallComponent;
		this.testingComponent = testingComponent;
		this.serviceLoader = serviceLoader;
		this.serviceStatusDetailsExporter = serviceStatusDetailsExporter;

		this.validateState();
		this.updateServices();
	}

	private void validateState() {

		Assert.notNull(pingComponent, "the pingComponent must not be null");
		Assert.notNull(methodCallComponent, "the methodCallComponent must not be null");
		Assert.notNull(testingComponent, "the testingComponent must not be null");
	}

	// TODO extract update methods to a new class
	// TODO abstract from component type, just have a list of components and iterate through them and call the methods
	/**
	 * This method is called regularly from Spring to update the available services using the Scheduled annotation.
	 */
	@Scheduled(cron = "${registry.updateInterval.cronValue}")
	protected void updateServices() {

		// TODO automatic nagios config generation needed, static service definition used till implemented
		List<Service> services = serviceLoader.loadServices();

		pingComponent.setServices(services);
		methodCallComponent.setServices(services);
		testingComponent.setServices(services);
	}

	@Scheduled(cron = "${pingComponent.updateInterval.cronValue}")
	protected void updatePingStatusAndExport() {
		updateAndExportStatus(pingComponent);
	}

	@Scheduled(cron = "${methodCallComponent.updateInterval.cronValue}")
	protected void updateMethodCallStatusAndExport() {
		updateAndExportStatus(methodCallComponent);
	}

	@Scheduled(cron = "${testingComponent.updateInterval.cronValue}")
	protected void updateTestingStatusAndExport() {
		updateAndExportStatus(testingComponent);
	}

	private void updateAndExportStatus(MonitoringComponent component) {

		component.updateStatus();

		List<ServiceStatusDetails> status = component.getServicesStatus();
		serviceStatusDetailsExporter.exportServiceStatusDetails(status);

		logExport(status);
	}

	private void logExport(List<ServiceStatusDetails> result) {
		if (logger.isDebugEnabled()) {
			logger.debug("refreshed component's cache");
			for (ServiceStatusDetails serviceStatusDetails : result) {
				logger.debug("service: " + serviceStatusDetails.getName() + " status: "
						+ serviceStatusDetails.getStatus().toString() + " response time: "
						+ serviceStatusDetails.getResponseTimeInMillis() + " ms");
			}
		}
	}

	@Override
	public List<ServiceStatusDetails> getPingStatus() {
		return pingComponent.getServicesStatus();
	}

	@Override
	public List<ServiceStatusDetails> getMethodCallStatus() {
		return methodCallComponent.getServicesStatus();
	}

	@Override
	public List<ServiceStatusDetails> getTestingStatus() {
		return testingComponent.getServicesStatus();
	}
}