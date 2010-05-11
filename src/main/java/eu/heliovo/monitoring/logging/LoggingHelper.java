package eu.heliovo.monitoring.logging;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Calendar;

import org.apache.log4j.Logger;

import com.eviware.soapui.model.testsuite.AssertionError;

public final class LoggingHelper {

	private LoggingHelper() {
	}

	public static void deleteLogFilesOlderThanOneDay(final String logFilesDirectory, final Logger logger) {

		final File[] oldLogFiles = new File(logFilesDirectory).listFiles(new LogFilenameFilter());
		final Calendar now = Calendar.getInstance();

		if (oldLogFiles != null) {
			for (int i = 0; i < oldLogFiles.length; i++) {
				if (isDifferenceGreaterThanSevenDays(now.getTimeInMillis(), oldLogFiles[i].lastModified())) {
					try {
						oldLogFiles[i].delete();
					} catch (final SecurityException e) {
						logger.info("log file " + oldLogFiles[i].toString() + " could not be deleted", e);
					}
				}
			}
		}
	}

	private static boolean isDifferenceGreaterThanSevenDays(final long milliseconds1, final long milliseconds2) {
		return Math.abs(calculateDiffernceInDays(milliseconds1, milliseconds2)) > 7;
	}

	private static long calculateDiffernceInDays(final long milliseconds1, final long milliseconds2) {
		return (milliseconds1 - milliseconds2) / (1000 * 60 * 60 * 24);
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

	public static void logRequestValidation(final AssertionError[] assertionErrors, final LogFileWriter logFileWriter,
			final Logger logger) {
		logValidationErrors(assertionErrors, logFileWriter, logger, "request");
	}

	public static void logResponseValidation(final AssertionError[] assertionErrors, final LogFileWriter logFileWriter,
			final Logger logger) {
		logValidationErrors(assertionErrors, logFileWriter, logger, "response");
	}

	private static void logValidationErrors(final AssertionError[] assertionErrors, final LogFileWriter logFileWriter,
			final Logger logger, final String validationObjectName) {

		logger.debug("Validating " + validationObjectName);
		logFileWriter.writeToLogFile("Validating " + validationObjectName);

		if (assertionErrors == null || assertionErrors.length < 1) {
			logger.debug("No validation erros found");
			logFileWriter.writeToLogFile("No validation erros found");
		} else {

			logger.debug("The " + validationObjectName + " has validation erros:");
			logFileWriter.writeToLogFile("The " + validationObjectName + " has validation erros:");

			for (int i = 0; i < assertionErrors.length; i++) {
				logger.debug(assertionErrors[i].toString());
				logFileWriter.writeToLogFile(assertionErrors[i].toString());
			}
		}
	}

	private final static class LogFilenameFilter implements FilenameFilter {
		@Override
		public boolean accept(final File dir, final String name) {
			return name.endsWith(LogFileWriter.FILE_SUFFIX);
		}
	}

}
