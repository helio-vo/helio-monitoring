package eu.heliovo.monitoring.model;

import java.net.URL;
import java.util.*;

import org.springframework.util.StringUtils;

/**
 * Only with this factory instances of the model package should be created to be able to easily replace the
 * implementations (factory method pattern). Implementations should be immutable to garantee robustness.
 * 
 * @author Kevin Seidler
 * 
 */
public final class ModelFactory {

	private ModelFactory() {
	}

	public static Host newHost(URL url, Set<Service> services) {
		return new HostImpl(url, services);
	}

	public static Service newService(final String name, final URL url) {
		return new ServiceImpl(name, url);
	}

	public static TestingService newService(final String name, final URL url, final List<OperationTest> operationTests) {
		return new TestingServiceImpl(name, url, operationTests);
	}

	public static OperationTest newOperationTest(final String operationName, final String requestContent) {
		return ModelFactory.newOperationTest(operationName, requestContent, null);
	}

	public static OperationTest newOperationTestWithoutRequest(final String operationName, final String responseContent) {
		return ModelFactory.newOperationTest(operationName, null, responseContent);
	}

	public static OperationTest newOperationTest(final String operationName, final String requestContent,
			final String responseContent) {

		if (!StringUtils.hasText(operationName)) {
			throw new IllegalArgumentException("operationName must not be empty!");
		}

		return new OperationTestImpl(operationName, requestContent, responseContent);
	}

	public static ServiceStatusDetails newServiceStatusDetails(String id, URL url, ServiceStatus status,
			long responseTimeInMillis, String message) {

		return new ServiceStatusDetailsImpl(id, url, status, responseTimeInMillis, message);
	}
}