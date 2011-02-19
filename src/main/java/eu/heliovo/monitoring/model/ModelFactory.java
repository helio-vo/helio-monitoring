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

	public static Service newService(String identifier, String name, URL url) {
		return new ServiceImpl(identifier, name, url);
	}

	public static TestingService newService(String identifier, String name, URL url, List<OperationTest> operationTests) {
		return new TestingServiceImpl(identifier, name, url, operationTests);
	}

	public static OperationTest newOperationTest(String operationName, String requestContent) {
		return ModelFactory.newOperationTest(operationName, requestContent, null);
	}

	public static OperationTest newOperationTestWithoutRequest(String operationName, String responseContent) {
		return ModelFactory.newOperationTest(operationName, null, responseContent);
	}

	public static OperationTest newOperationTest(String operationName, String requestContent,
			final String responseContent) {

		if (!StringUtils.hasText(operationName)) {
			throw new IllegalArgumentException("operationName must not be empty!");
		}

		return new OperationTestImpl(operationName, requestContent, responseContent);
	}

	public static <MonitoredEntity> StatusDetails<MonitoredEntity> newStatusDetails(MonitoredEntity monitoredEntity,
			String name, URL url, Status status, long responseTimeInMillis, String message) {

		return new StatusDetailsImpl<MonitoredEntity>(monitoredEntity, name, url, status, responseTimeInMillis, message);
	}
}