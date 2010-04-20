package eu.heliovo.monitoring.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.apache.log4j.Logger;

public class LogFileWriter {

	private final File mainDir;
	private FileWriter fileWriter;
	private final String fileName;

	protected Logger logger = Logger.getLogger(this.getClass());

	public LogFileWriter(final String directory, final String name) {

		mainDir = new File(directory);
		if (!mainDir.exists()) {
			try {
				mainDir.mkdirs();
			} catch (final SecurityException e) {
				logger.error(e.getMessage(), e);
			}
		}

		final StringBuffer buffer = new StringBuffer(mainDir.getPath());
		buffer.append("/");
		buffer.append(name);
		buffer.append("-");
		buffer.append(System.currentTimeMillis());
		buffer.append(".txt");
		this.fileName = buffer.toString();
	}

	public void writeToLogFile(final String text) {
		try {

			if (fileWriter == null) {
				fileWriter = new FileWriter(fileName);
			}

			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ", Locale.UK);
			final String timeStamp = dateFormat.format(Calendar.getInstance().getTime());

			fileWriter.append(timeStamp + text + "\n");

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			fileWriter.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public String getFileName() {
		return new File(fileName).getName();
	}
}
