package eu.heliovo.monitoring.logging;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.*;

/**
 * This implementation of logFileWriter writes log files by a log4j logger and fileAppender in XML layout. <br/>
 * TODO create own XML layout? layout is log4j specific and has a bit overhead, see commented code
 * 
 * @author Kevin Seidler
 * 
 */
public final class Log4JLogFileWriter implements LogFileWriter {

	// XML header and footer must be written manually
	// private static final String LOG4J_XML_HEADER;
	// private static final String LOG4J_XML_FOOTER = "</log4j:eventSet>";

	private final String fileName;
	private final Logger logger = Logger.getLogger(this.getClass());
	private final FileAppender fileAppender;

	// static {
	// LOG4J_XML_HEADER = getLog4JXmlHeader();
	// }

	protected Log4JLogFileWriter(String logFilesdirectory, String name) {

		createLogFileDirectory(logFilesdirectory);
		fileName = getFileName(logFilesdirectory, name);

		// addLog4JXmlHeader();

		fileAppender = getLogFileAppender();
		logger.addAppender(fileAppender);
	}

	private File createLogFileDirectory(String logFilesdirectoryPath) {
		File logFilesdirectory = new File(logFilesdirectoryPath);
		if (!logFilesdirectory.exists()) {
			try {
				logFilesdirectory.mkdirs();
			} catch (SecurityException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
		return logFilesdirectory;
	}

	private String getFileName(String logFilesdirectory, String name) {

		StringBuffer buffer = new StringBuffer(logFilesdirectory);
		buffer.append(System.getProperty("file.separator"));
		buffer.append(name);
		buffer.append("-");
		buffer.append(System.currentTimeMillis());
		buffer.append(FILE_SUFFIX);

		return buffer.toString();
	}

	private FileAppender getLogFileAppender() {
		// Layout layout = new XMLLayout();
		Layout layout = new PatternLayout();
		try {
			return new FileAppender(layout, fileName);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	// private static String getLog4JXmlHeader() {
	// StringBuffer header = new StringBuffer();
	// header.append("<?xml version=\"1.0\" ?>\n");
	// header.append("<!DOCTYPE log4j:eventSet PUBLIC \"-//APACHE//DTD LOG4J 1.2//EN\" ");
	// header.append("\"log4j.dtd\" [<!ENTITY data SYSTEM \"abc\">]>\n");
	// header.append("<log4j:eventSet version=\"1.2\" xmlns:log4j=\"http://jakarta.apache.org/log4j/\">\n\n");
	// return header.toString();
	// }

	// private void addLog4JXmlHeader() {
	// try {
	// FileWriter fileWriter = new FileWriter(fileName);
	// fileWriter.append(LOG4J_XML_HEADER);
	// fileWriter.close();
	// } catch (IOException e) {
	// throw new IllegalStateException(e.getMessage(), e);
	// }
	// }
	//
	// private void addLog4JXmlFooter() {
	// try {
	// FileWriter fileWriter = new FileWriter(fileName, true);
	// fileWriter.append(LOG4J_XML_FOOTER);
	// fileWriter.close();
	// } catch (IOException e) {
	// throw new IllegalStateException(e.getMessage(), e);
	// }
	// }

	@Override
	public void write(String text) {

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ", Locale.UK);
		String timeStamp = dateFormat.format(Calendar.getInstance().getTime());

		logger.debug(timeStamp + text);
	}

	@Override
	public void write(Exception e) {
		logger.debug("An error occured: " + e.getMessage(), e);
	}

	@Override
	public void close() {
		logger.removeAppender(fileAppender);
		fileAppender.close();
		// addLog4JXmlFooter();
	}

	@Override
	public String getFileName() {
		return new File(fileName).getName();
	}
}