package eu.heliovo.monitoring.logging;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Returns implementations of the logging package.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class LoggingFactory {

	private final String logFilesDirectory;

	@Autowired
	protected LoggingFactory(@Value("${logging.filePath}") String logFilesDirectory) {
		this.logFilesDirectory = logFilesDirectory;
	}

	/**
	 * Creates a LogFileWriter writing a log file with the given name as part of the file name.
	 * 
	 * @param name This name is part of the file name.
	 * @return
	 */
	public LogFileWriter newLogFileWriter(String name) {
		try {
			return new Log4JLogFileWriter(logFilesDirectory, name);
		} catch (RuntimeException e) {
			Logger logger = Logger.getLogger(LoggingFactory.class);
			logger.warn("LogFileWriter could not be created: " + e.getMessage() + " using Dummy", e);
			return new DummyLogFileWriter();
		}
	}
}