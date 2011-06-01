package eu.heliovo.monitoring.serviceloader;

import static eu.heliovo.monitoring.model.ModelFactory.newService;
import static org.springframework.util.StringUtils.hasText;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;

import uk.ac.starlink.registry.*;
import eu.heliovo.monitoring.model.Service;

/**
 * For retrieving registered Services from an IVOA Registry.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class IvoaRegistryServiceLoader implements ServiceLoader {

//	private static final String ADQLS_QUERY = "capability/@standardID='ivo://helio-vo.eu/std/FullQuery/v0.2'";
	private static final String ADQLS_QUERY = "capability/interface/@xsi:type='vr:WebService'";
	
	private static final int SOAP_SERVICE_INDEX = 1;
	private static final String INTERFACE_NAME = "HelioService";
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
	@Override
	public Set<Service> loadServices() {

		// TODO get services from registry, if registry down => no services, if services successfully retrieved in the
		// past, use these old infos till registry on again and display offline/broken registry in nagios

		BasicRegistryClient registryClient = new BasicRegistryClient(new SoapClient(registryUrl));
		try {
			// construct the SOAP request corresponding to the services query
			SoapRequest soapRequest = RegistryRequestFactory.adqlsSearch(ADQLS_QUERY);

			Iterator<BasicResource> iterator = callRegistryAndGetIterator(registryClient, soapRequest);

			Set<Service> services = new HashSet<Service>();
			while (iterator.hasNext()) {

				BasicResource registryResource = iterator.next();
				try {
					Service service = readService(registryResource);
					services.add(service);
				} catch (MalformedURLException e) {
					logger.warn("service URL was malformed, service could not be added", e);
				}
			}
			return Collections.unmodifiableSet(services);

			// TODO write logs? derive registry status for monitoring? force registry check?
		} catch (Exception e) {
			logger.warn("services could not be retrieved from the registry", e);
		}
		return Collections.emptySet();
	}

	private Iterator<BasicResource> callRegistryAndGetIterator(final BasicRegistryClient registryClient,
			final SoapRequest soapRequest) throws InterruptedException, ExecutionException, TimeoutException {

		Future<Iterator<BasicResource>> future = executor.submit(new Callable<Iterator<BasicResource>>() {
			@Override
			public Iterator<BasicResource> call() throws IOException {
				return registryClient.getResourceIterator(soapRequest);
			}
		});

		// TODO automatically determine timeout
		// TODO registryClient.getResourceIterator(soapRequest) creates another thread which opens a URLConnection which
		// cannot be interupted and stays open till default connection timeout occurs. how to interrupt this one?
		try {
			return future.get(RESPONSE_TIMEOUT, TimeUnit.SECONDS);
		} finally {
			future.cancel(true);
		}
	}

	private Service readService(BasicResource registryResource) throws MalformedURLException {

		String serviceName = registryResource.getTitle();
		String serviceIdentifier = registryResource.getIdentifier();

		BasicCapability[] capabilities = registryResource.getCapabilities();
		String serviceUrl = getServiceUrl(capabilities);

		if (!serviceUrl.toLowerCase().endsWith(WSDL_SUFFIX)) {
			serviceUrl += WSDL_SUFFIX;
		}

		return newService(serviceIdentifier, serviceName, new URL(serviceUrl));
	}

	private String getServiceUrl(BasicCapability[] capabilities) {
	    // always return the first capability with a url assigned.
	    for (BasicCapability basicCapability : capabilities) {
			String serviceUrl = basicCapability.getAccessUrl();
			if (hasText(serviceUrl)) {
				return serviceUrl;
			}
		}
		return searchForServiceUrl(capabilities); // if the url is registered at the wrong place
	}

	private String searchForServiceUrl(BasicCapability[] capabilities) {

		for (BasicCapability basicCapability : capabilities) {
			String serviceUrl = basicCapability.getAccessUrl();
			if (hasText(serviceUrl) && serviceUrl.endsWith(INTERFACE_NAME)) {
				return serviceUrl;
			}
		}
		return ""; // leading to a MalformedURLException and not adding this service
	}
}