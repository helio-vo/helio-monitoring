package eu.heliovo.monitoring.logging;

import org.apache.log4j.Logger;

public final class LoggingFactory {

	private LoggingFactory() {
	}

	public static LogFileWriter newLogFileWriter(String logFilesDirectory, String name) {
		try {
			return new LogFileWriterImpl(logFilesDirectory, name);
		} catch (RuntimeException e) {
			Logger logger = Logger.getLogger(LoggingFactory.class);
			logger.warn("LogFileWriter could not be created: " + e.getMessage() + " using Dummy", e);
			return new DummyLogFileWriter();
		}
	}
}
