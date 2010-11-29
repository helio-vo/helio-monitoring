package eu.heliovo.monitoring.logging;

public interface LogFileWriter {

	String FILE_SUFFIX = ".txt";

	void writeToLogFile(final String text);

	void writeStacktracetoLogFile(Exception e);

	void close();

	String getFileName();

}