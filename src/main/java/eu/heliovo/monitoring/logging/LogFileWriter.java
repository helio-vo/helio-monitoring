package eu.heliovo.monitoring.logging;

public interface LogFileWriter {

	static final String FILE_SUFFIX = ".txt";

	abstract void writeToLogFile(final String text);

	abstract void writeStacktracetoLogFile(Exception e);

	abstract void close();

	abstract String getFileName();

}