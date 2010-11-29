package eu.heliovo.monitoring.test.util;

import java.util.concurrent.ExecutorService;

import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import eu.heliovo.monitoring.component.ComponentHelper;

public final class TestUtils {

	public static final String logFilesUrl = "http://localhost:8080/helio-monitoring/logs";

	private TestUtils() {
	}

	public static ExecutorService getExecutor() throws Exception {
		ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
		factory.afterPropertiesSet();
		ExecutorService executor = factory.getObject();
		return executor;
	}

	public static ComponentHelper getComponentHelper() throws Exception {
		return new ComponentHelper(getExecutor(), logFilesUrl);
	}

}