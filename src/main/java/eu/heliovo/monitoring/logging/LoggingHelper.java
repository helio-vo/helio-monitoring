package eu.heliovo.monitoring.logging;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.eviware.soapui.model.testsuite.AssertionError;

@Component
public final class LoggingHelper {

	private static final int SEVEN = 7;
	private static final int TO_DAYS_DIVISOR = 1000 * 60 * 60 * 24;

	private static final Logger LOGGER = Logger.getLogger(LoggingHelper.class);

	private final String logFilesDirectory;

	@Autowired
	public LoggingHelper(@Value("${logging.filePath}") String logFilesDirectory) {
		this.logFilesDirectory = logFilesDirectory;
	}

	@Scheduled(cron = "${logging.deleteFilesInterval.cronValue}")
	public void deleteLogFilesOlderThanOneWeek() {

		final File[] oldLogFiles = new File(logFilesDirectory).listFiles(new LogFilenameFilter());
		final Calendar now = Calendar.getInstance();

		if (oldLogFiles != null) {
			for (int i = 0; i < oldLogFiles.length; i++) {
				if (isDifferenceGreaterThanSevenDays(now.getTimeInMillis(), oldLogFiles[i].lastModified())) {
					try {
						oldLogFiles[i].delete();
					} catch (final SecurityException e) {
						LOGGER.info("log file " + oldLogFiles[i].toString() + " could not be deleted", e);
					}
				}
			}
		}
	}

	private static boolean isDifferenceGreaterThanSevenDays(final long milliseconds1, final long milliseconds2) {
		return Math.abs(calculateDiffernceInDays(milliseconds1, milliseconds2)) > SEVEN;
	}

	private static long calculateDiffernceInDays(final long milliseconds1, final long milliseconds2) {
		return (milliseconds1 - milliseconds2) / TO_DAYS_DIVISOR;
	}

	public static String getLogFileText(final LogFileWriter logFileWriter, final String logFilesUrl) {

		if (DummyLogFileWriter.class.equals(logFileWriter.getClass())) {
			return "";
		}

		final StringBuffer buffer = new StringBuffer();
		buffer.append(", see log file: ");
		buffer.append(logFilesUrl);
		buffer.append("/");
		buffer.append(logFileWriter.getFileName());

		return buffer.toString();
	}

	public static void logRequestValidation(final AssertionError[] assertionErrors, final LogFileWriter logFileWriter) {
		logValidationErrors(assertionErrors, logFileWriter, "request");
	}

	public static void logResponseValidation(final AssertionError[] assertionErrors, final LogFileWriter logFileWriter) {
		logValidationErrors(assertionErrors, logFileWriter, "response");
	}

	private static void logValidationErrors(final AssertionError[] assertionErrors, final LogFileWriter logFileWriter,
			final String validationObjectName) {

		LOGGER.debug("Validating " + validationObjectName);
		logFileWriter.writeToLogFile("Validating " + validationObjectName);

		if (assertionErrors == null || assertionErrors.length < 1) {
			LOGGER.debug("No validation erros found");
			logFileWriter.writeToLogFile("No validation erros found");
		} else {

			LOGGER.debug("The " + validationObjectName + " has validation erros:");
			logFileWriter.writeToLogFile("The " + validationObjectName + " has validation erros:");

			for (int i = 0; i < assertionErrors.length; i++) {
				LOGGER.debug(assertionErrors[i].toString());
				logFileWriter.writeToLogFile(assertionErrors[i].toString());
			}
		}
	}

	private static final class LogFilenameFilter implements FilenameFilter {
		@Override
		public boolean accept(final File dir, final String name) {
			return name.endsWith(LogFileWriter.FILE_SUFFIX);
		}
	}

}
