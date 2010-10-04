package eu.heliovo.monitoring.logging;

import org.apache.log4j.Logger;

public final class LoggingFactory {

	private LoggingFactory() {
	}

	public static final LogFileWriter newLogFileWriter(final String logFilesDirectory, final String name) {
		try {
			return new LogFileWriterImpl(logFilesDirectory, name);
		} catch (final RuntimeException e) {
			final Logger logger = Logger.getLogger(LoggingFactory.class);
			logger.warn("LogFileWriter could not be created: " + e.getMessage() + " using Dummy", e);
			return new DummyLogFileWriter();
		}
	}
}
