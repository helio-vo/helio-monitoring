package eu.heliovo.monitoring.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.heliovo.monitoring.component.MethodCallComponent;
import eu.heliovo.monitoring.component.PingComponent;
import eu.heliovo.monitoring.component.TestingComponent;
import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.statics.Services;

/**
 * The MonitoringService instatiated as web service. Does only provide getStatus
 * with services, their state (up or down) and response time if up.
 * 
 * @author Kevin Seidler
 * 
 */
@Service
public class MonitoringServiceImpl implements MonitoringService, InitializingBean {

	// services from the registry with ID and URL
	private List<eu.heliovo.monitoring.model.Service> services = new ArrayList<eu.heliovo.monitoring.model.Service>();

	private final PingComponent pingComponent;
	private final MethodCallComponent methodCallComponent;
	private final TestingComponent testingComponent;

	@Autowired
	public MonitoringServiceImpl(final PingComponent pingComponent, final MethodCallComponent methodCallComponent,
			final TestingComponent testingComponent) {
		this.pingComponent = pingComponent;
		this.methodCallComponent = methodCallComponent;
		this.testingComponent = testingComponent;
	}

	@Override
	public void afterPropertiesSet() {

		Assert.notNull(pingComponent, "the pingComponent must not be null");
		Assert.notNull(methodCallComponent, "the methodCallComponent must not be null");

		readServicesFromRegistry();

		pingComponent.setServices(services);
		methodCallComponent.setServices(services);
		testingComponent.setServices(services);
	}

	/**
	 * Reads the actual services from the Helio Registry Service. This method
	 * gets called autmatically by a quartz job from the application context.
	 * TODO implement this with the real Helio Registry Service<br>
	 */
	private void readServicesFromRegistry() {
		services = Services.LIST;
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