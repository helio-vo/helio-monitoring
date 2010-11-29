package eu.heliovo.monitoring.service;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Assert;

import eu.heliovo.monitoring.component.AbstractComponent;
import eu.heliovo.monitoring.component.MethodCallComponent;
import eu.heliovo.monitoring.component.PingComponent;
import eu.heliovo.monitoring.component.TestingComponent;
import eu.heliovo.monitoring.daemon.RemotingMonitoringDaemon;
import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.serviceloader.ServiceLoader;

/**
 * The MonitoringService instatiated as web service. Does only provide getStatus with services, their state (up or down)
 * and response time if up.
 * 
 * @author Kevin Seidler
 * 
 */
@org.springframework.stereotype.Service
public final class MonitoringServiceImpl implements MonitoringService, InitializingBean {

	private final Logger logger = Logger.getLogger(this.getClass());

	private final PingComponent pingComponent;
	private final MethodCallComponent methodCallComponent;
	private final TestingComponent testingComponent;

	private final ServiceLoader serviceLoader;

	private final RemotingMonitoringDaemon daemon;

	// TODO Spring EL not working in @Qualifier, therefore both daemons are passed with property value for the right
	// one. Fix this when possbile.
	@Autowired
	public MonitoringServiceImpl(PingComponent pingComponent, MethodCallComponent methodCallComponent,
			TestingComponent testingComponent, @Qualifier("staticServiceLoader") ServiceLoader serviceLoader,
			@Qualifier("monitoringDaemon") RemotingMonitoringDaemon monitoringDaemon,
			@Qualifier("remotingMonitoringDaemon") RemotingMonitoringDaemon remotingMonitoringDaemon,
			@Value("${monitoringDaemonSpringBeanId}") String activeDaemonId) {

		this.pingComponent = pingComponent;
		this.methodCallComponent = methodCallComponent;
		this.testingComponent = testingComponent;
		this.serviceLoader = serviceLoader;
		this.daemon = "monitoringDaemon".equals(activeDaemonId) ? monitoringDaemon : remotingMonitoringDaemon;
	}

	@Override
	public void afterPropertiesSet() {

		Assert.notNull(pingComponent, "the pingComponent must not be null");
		Assert.notNull(methodCallComponent, "the methodCallComponent must not be null");
		Assert.notNull(testingComponent, "the testingComponent must not be null");
	}

	// TODO extract update methods to a new class
	/**
	 * This method is called regularly from Spring to update the available services using the Scheduled annotation.
	 */
	@Scheduled(cron = "${registry.updateInterval.cronValue}")
	protected void updateServices() {

		// TODO automatic nagios config generation needed
		// List<Service> services = registryClient == null ? Services.LIST :
		// registryClient.retrieveServicesFromRegistry();

		List<Service> services = serviceLoader.loadServices();

		pingComponent.setServices(services);
		methodCallComponent.setServices(services);
		testingComponent.setServices(services);
	}

	@Scheduled(cron = "${pingComponent.updateInterval.cronValue}")
	protected void updatePingStatusAndWriteToNagios() {
		updateStatusAndWriteToNagios(pingComponent, daemon);
	}

	@Scheduled(cron = "${methodCallComponent.updateInterval.cronValue}")
	protected void updateMethodCallStatusAndWriteToNagios() {
		updateStatusAndWriteToNagios(methodCallComponent, daemon);
	}

	@Scheduled(cron = "${testingComponent.updateInterval.cronValue}")
	protected void updateTestingStatusAndWriteToNagios() {
		updateStatusAndWriteToNagios(testingComponent, daemon);
	}

	private void updateStatusAndWriteToNagios(AbstractComponent component, RemotingMonitoringDaemon monitoringDaemon) {

		component.refreshCache();

		final List<ServiceStatus> result = component.getStatus();
		monitoringDaemon.writeServiceStatusToNagios(result);

		if (logger.isDebugEnabled()) {
			logger.debug("refreshed component's cache");
			for (final ServiceStatus serviceStatus : result) {
				logger.debug("service: " + serviceStatus.getId() + " status: " + serviceStatus.getState().toString()
						+ " response time: " + serviceStatus.getResponseTime() + " ms");
			}
		}
	}

	@Override
	public List<ServiceStatus> getPingStatus() {
		return pingComponent.getStatus();
	}

	@Override
	public List<ServiceStatus> getMethodCallStatus() {
		return methodCallComponent.getStatus();
	}

	@Override
	public List<ServiceStatus> getTestingStatus() {
		return testingComponent.getStatus();
	}
}