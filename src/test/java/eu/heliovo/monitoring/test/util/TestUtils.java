package eu.heliovo.monitoring.test.util;

import java.util.concurrent.*;

import org.junit.Ignore;
import org.springframework.scheduling.concurrent.*;

import eu.heliovo.monitoring.component.ComponentHelper;

@Ignore
public final class TestUtils {

	public static final String logFilesUrl = "http://localhost:8080/helio-monitoring/logs";

	private TestUtils() {
	}

	public static ExecutorService getExecutor() throws Exception {
		ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
		factory.setCorePoolSize(100); // size of services
		factory.setAllowCoreThreadTimeOut(true);
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	public static ScheduledExecutorService getScheduledExecutor() throws Exception {
		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		factory.setPoolSize(10);
		factory.setContinueScheduledExecutionAfterException(true);
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	public static ComponentHelper getComponentHelper() throws Exception {
		return new ComponentHelper(getExecutor(), logFilesUrl);
	}

}