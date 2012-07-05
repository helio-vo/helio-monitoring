package eu.heliovo.monitoring.service;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.model.StatusDetails;

/**
 * Represents the JAX-WS endpoint, it is needed for JAW-WS and Spring to work together and is registered as a servlet in
 * the web.xml. For a good tutorial see http://www.javacodegeeks.com/2010/11/jaxws-with-spring-and-maven-tutorial.html.
 * But Autowiring is not working with the DispatcherServlet and the ContextLoaderListener of Spring. As a workaround the
 * MonitoringService is injected statically.
 * 
 * @author Kevin Seidler
 * 
 */
@WebService(serviceName = "HelioService")
public class HelioServiceEndpoint extends SpringBeanAutowiringSupport {

    @Autowired
	private MonitoringService monitoringService;

	public HelioServiceEndpoint() {
    }
	
	/**
	 * Please see {@link MonitoringService#getAllStatus()}.
	 * 
	 * @return
	 */
	@WebMethod
	public List<StatusDetails<Service>> getAllStatus() {
		return monitoringService.getAllStatus();
	}

	/**
	 * Please see {@link MonitoringService#getStatus(String)}.
	 */
	@WebMethod
	public StatusDetails<Service> getStatus(@WebParam(name = "serviceIdendifier") String serviceIdendifier) {
		return monitoringService.getStatus(serviceIdendifier);
	}
}