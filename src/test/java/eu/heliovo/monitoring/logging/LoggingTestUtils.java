package eu.heliovo.monitoring.logging;

import org.junit.Ignore;

@Ignore
public class LoggingTestUtils {

	public static LoggingFactory getLoggingFactory() {
		return new LoggingFactory(System.getProperty("java.io.tmpdir"));
	}
}