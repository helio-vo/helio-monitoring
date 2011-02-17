package eu.heliovo.monitoring.service;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.*;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.*;
import org.springframework.scheduling.annotation.Scheduled;

import eu.heliovo.monitoring.listener.ServiceUpdateListener;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.serviceloader.ServiceLoader;
import eu.heliovo.monitoring.stage.StageExecutor;

/**
 * The MonitoringService instatiated as web service. Does only provide getStatus with services, their status (up or
 * down), a response time if up and some status information as a message.
 * 
 * @author Kevin Seidler
 * 
 */
@org.springframework.stereotype.Service
public final class MonitoringServiceImpl implements MonitoringService, ApplicationContextAware {

	private final ServiceLoader serviceLoader;
	private final StageExecutor stageExecutor;
	private final List<ServiceUpdateListener> serviceUpdateListeners;

	private int currentServicesHashCode = 0;

	// for manual service definition, please use "staticServiceLoader" as qualifier and define services in Services.java
	// Spring automatically injects all components implementing the ServiceUpdateListener interface
	@Autowired
	public MonitoringServiceImpl(@Qualifier("ivoaRegistryServiceLoader") ServiceLoader serviceLoader,
			StageExecutor stageExecutor, List<ServiceUpdateListener> serviceUpdateListeners) {

		this.serviceLoader = serviceLoader;
		this.stageExecutor = stageExecutor;
		this.serviceUpdateListeners = Collections.unmodifiableList(serviceUpdateListeners);
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

	@Override
	public List<StatusDetails<Service>> getStatus() {
		return stageExecutor.getStatus();
	}

	@Override
	public StatusDetails<Service> getStatus(String serviceId) {
		// TODO IMPLEMENT ME
		throw new UnsupportedOperationException("operation not supported yet");
	}

	/**
	 * This method is called after the application context was initialized. It updates the services to be monitored and
	 * starts the continous monitoring.
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		updateServices();
		stageExecutor.doContinousExecution();
	}
}