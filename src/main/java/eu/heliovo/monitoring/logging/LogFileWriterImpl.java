package eu.heliovo.monitoring.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.apache.log4j.Logger;

public final class LogFileWriterImpl implements LogFileWriter {

	private FileWriter fileWriter;
	private final String fileName;

	private final Logger logger = Logger.getLogger(this.getClass());

	public LogFileWriterImpl(final String directory, final String name) {

		final File mainDir = new File(directory);
		if (!mainDir.exists()) {
			try {
				mainDir.mkdirs();
			} catch (final SecurityException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		final StringBuffer buffer = new StringBuffer(mainDir.getPath());
		buffer.append("/");
		buffer.append(name);
		buffer.append("-");
		buffer.append(System.currentTimeMillis());
		buffer.append(FILE_SUFFIX);
		this.fileName = buffer.toString();
	}

	/* (non-Javadoc)
	 * @see eu.heliovo.monitoring.util.LogFileWriter#writeToLogFile(java.lang.String)
	 */
	public void writeToLogFile(final String text) {
		try {

			if (fileWriter == null) {
				fileWriter = new FileWriter(fileName);
			}

			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ", Locale.UK);
			final String timeStamp = dateFormat.format(Calendar.getInstance().getTime());

			fileWriter.append(timeStamp + text + "\n");

		} catch (final IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public void writeStacktracetoLogFile(final Exception e) {
		e.printStackTrace(new PrintWriter(fileWriter));
	}

	/* (non-Javadoc)
	 * @see eu.heliovo.monitoring.util.LogFileWriter#close()
	 */
	public void close() {
		try {
			fileWriter.close();
		} catch (final IOException e) {
			logger.info(e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see eu.heliovo.monitoring.util.LogFileWriter#getFileName()
	 */
	public String getFileName() {
		return new File(fileName).getName();
	}
}
