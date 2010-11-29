package eu.heliovo.monitoring.serviceloader;

import static eu.heliovo.monitoring.model.ServiceFactory.newService;
import static org.springframework.util.StringUtils.hasText;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import uk.ac.starlink.registry.BasicCapability;
import uk.ac.starlink.registry.BasicRegistryClient;
import uk.ac.starlink.registry.BasicResource;
import uk.ac.starlink.registry.RegistryRequestFactory;
import uk.ac.starlink.registry.SoapClient;
import uk.ac.starlink.registry.SoapRequest;
import eu.heliovo.monitoring.model.Service;

/**
 * For retrieving registered Services from a IVOA Registry.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class IvoaRegistryServiceLoader implements ServiceLoader {

	private static final String ADQLS_QUERY = "capability LIKE 'helio'";
	private static final int SOAP_SERVICE = 1;
	private static final String WSDL_SUFFIX = "?wsdl";
	private static final int RESPONSE_TIMEOUT = 10;

	private final Logger logger = Logger.getLogger(this.getClass());

	private final URL registryUrl;
	private final ExecutorService executor;

	@Autowired
	public IvoaRegistryServiceLoader(@Value("${registry.url}") String registryUrl, ExecutorService executor) {
		try {
			this.registryUrl = new URL(registryUrl);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("registry URL must be well formatted", e);
		}
		this.executor = executor;
	}

	/**
	 * Reads the actual services from the Registry Service.
	 */
	public List<Service> loadServices() {

		// TODO get services from registry, if registry down => no services, if services successfully retrived in the
		// past, use these old infos till registry on again and display offline/broken registry in nagios

		BasicRegistryClient registryClient = new BasicRegistryClient(new SoapClient(registryUrl));
		try {
			// construct the SOAP request corresponding to the services query
			SoapRequest soapRequest = RegistryRequestFactory.adqlsSearch(ADQLS_QUERY);

			Iterator<BasicResource> iterator = callRegistryAndGetIterator(registryClient, soapRequest);

			List<Service> services = new ArrayList<Service>();
			while (iterator.hasNext()) {
				
				BasicResource registryResource = iterator.next();
				try {
					Service service = readService(registryResource);
					services.add(service);
				} catch (MalformedURLException e) {
					logger.warn("service URL was malformed, service could not be added", e);
				}
			}
			return Collections.unmodifiableList(services);

			// TODO write logs? derive registry status for monitoring? force registry check?
		} catch (Exception e) {
			logger.warn("services could not be retrieved from the registry", e);
		}

		return Collections.emptyList();
	}

	private Iterator<BasicResource> callRegistryAndGetIterator(final BasicRegistryClient registryClient,
			final SoapRequest soapRequest) throws InterruptedException, ExecutionException, TimeoutException {

		Future<Iterator<BasicResource>> future = executor.submit(new Callable<Iterator<BasicResource>>() {
			public Iterator<BasicResource> call() throws IOException {
				return registryClient.getResourceIterator(soapRequest);
			}
		});

		// TODO automatically determine timeout
		return future.get(RESPONSE_TIMEOUT, TimeUnit.SECONDS);
	}

	private Service readService(BasicResource registryResource) throws MalformedURLException {

		String resourceShortName = registryResource.getShortName();
		String identifier = registryResource.getIdentifier();
		String serviceName = hasText(resourceShortName) ? resourceShortName : cleanIdentifier(identifier);

		BasicCapability[] capabilities = registryResource.getCapabilities();
		String serviceUrl = capabilities[SOAP_SERVICE].getAccessUrl();

		if (!serviceUrl.toLowerCase().endsWith(WSDL_SUFFIX)) {
			serviceUrl += WSDL_SUFFIX;
		}

		return newService(serviceName, new URL(serviceUrl));
	}

	private String cleanIdentifier(String identifier) {
		return identifier.substring(identifier.lastIndexOf('/') + 1, identifier.length());
	}
}
