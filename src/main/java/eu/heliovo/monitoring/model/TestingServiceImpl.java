package eu.heliovo.monitoring.model;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public final class TestingServiceImpl implements TestingService {

	private final String name;
	private final URL url;
	private final List<OperationTest> operationTests;

	protected TestingServiceImpl(final String name, final URL url, final List<OperationTest> operationTests) {
		this.name = name;
		this.url = url;
		this.operationTests = Collections.unmodifiableList(operationTests);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public URL getUrl() {
		return url;
	}

	@Override
	public List<OperationTest> getOperationTests() {
		return operationTests;
	}

}
