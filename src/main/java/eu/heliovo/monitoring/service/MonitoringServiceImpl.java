package eu.heliovo.monitoring.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.heliovo.monitoring.component.PingComponent;
import eu.heliovo.monitoring.model.ServiceStatus;

/**
 * The MonitoringService instatiated as web service. Does only provide getStatus
 * with services, their state (up or down) and response time if up.
 * 
 * @author Kevin Seidler
 * 
 */
@Service("monitoringService")
public class MonitoringServiceImpl implements MonitoringService, InitializingBean {

	// services from the registry with ID and URL
	private final Map<String, URL> services = new HashMap<String, URL>();

	private final PingComponent pingComponent;

	@Autowired
	public MonitoringServiceImpl(final PingComponent pingComponent) {
		this.pingComponent = pingComponent;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(pingComponent, "the pingComponent must not be null");
		readServicesFromRegistry();
		pingComponent.setServices(services);
	}

	/**
	 * Reads the actual services from the Helio Registry Service. This method
	 * gets called autmatically by a quartz job from the application context.
	 * TODO implement this with the real Helio Registry Service<br>
	 */
	private void readServicesFromRegistry() {
		try {
			services.put("local HEC", new URL("http://localhost:8080/core/HECService?wsdl"));
			services.put("local FrontendFacade", new URL("http://localhost:8080/core/FrontendFacadeService?wsdl"));

			services.put("HEC", new URL("http://helio.i4ds.technik.fhnw.ch:8080/core/HECService?wsdl"));
			services.put("FrontendFacade", new URL(
					"http://helio.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl"));
			services.put("WorkflowsService",
					new URL("http://helio.i4ds.technik.fhnw.ch/helio-wf/WorkflowsService?wsdl"));

			services.put("helio-dev HEC", new URL("http://helio-dev.i4ds.technik.fhnw.ch:8080/core/HECService?wsdl"));
			services.put("helio-dev FrontendFacade", new URL(
					"http://helio-dev.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl"));
			services.put("helio-dev WorkflowsService", new URL(
					"http://helio-dev.i4ds.technik.fhnw.ch/helio-wf/WorkflowsService?wsdl"));

			services.put("FakeOfflineService", new URL("http://123.43.121.11/"));

		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<ServiceStatus> getPingStatus() {
		return pingComponent.getStatus();
	}
}