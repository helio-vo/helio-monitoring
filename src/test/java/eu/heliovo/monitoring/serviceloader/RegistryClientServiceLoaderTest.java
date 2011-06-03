package eu.heliovo.monitoring.serviceloader;

import java.util.Collection;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.registryclient.AccessInterface;
import eu.heliovo.registryclient.ServiceCapability;
import eu.heliovo.registryclient.ServiceDescriptor;
import eu.heliovo.registryclient.ServiceRegistryClient;
import eu.heliovo.registryclient.impl.ServiceRegistryClientFactory;

public class RegistryClientServiceLoaderTest extends Assert {

	private static final String REGISTRY_TABLE_FORMAT = "| %-80s | %-50s | %-20s | %-90s |";

    public RegistryClientServiceLoaderTest() throws Exception {
	}

	@Test
	public void testLoadServices() throws Exception {

	    System.out.println("-------------------------------------------------------------------------------------------------------------------");
		ServiceLoader serviceloader = new RegistryClientServiceLoader();
		Set<Service> services = serviceloader.loadServices();
		for (Service service : services) {
			System.out.println(service.getIdentifier() + " - " + service.getName() + " - " + service.getUrl());
		}
		System.out.println("-------------------------------------------------------------------------------------------------------------------");
		Collection<ServiceCapability> vals = ServiceCapability.values();
		for (ServiceCapability serviceCapability : vals) {
            System.out.println(String.format("| %-50s | %-50s |", serviceCapability.getStandardId(), serviceCapability.getName()));
        }
		
		System.out.println("-------------------------------------------------------------------------------------------------------------------");
		
	    // get a pointer to the service registry client
        ServiceRegistryClient serviceRegistryClient = ServiceRegistryClientFactory.getInstance().getServiceRegistryClient();
        
        System.out.println(String.format(REGISTRY_TABLE_FORMAT, "service", "capability", "xsi:Type", "url"));
        ServiceDescriptor[] serviceDescriptors = serviceRegistryClient.getAllServiceDescriptors();
        for (ServiceDescriptor serviceDescriptor : serviceDescriptors) {
            AccessInterface[] soapServices = serviceRegistryClient.getAllEndpoints(serviceDescriptor, null, null);
            for (AccessInterface accessInterface : soapServices) {
                System.out.println(String.format(REGISTRY_TABLE_FORMAT, serviceDescriptor.getName(), accessInterface.getCapability(), accessInterface.getInterfaceType(), accessInterface.getUrl()));
            }
        }
   
	}
}