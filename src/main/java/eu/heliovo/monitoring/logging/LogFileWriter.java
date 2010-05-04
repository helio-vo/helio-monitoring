package eu.heliovo.monitoring.logging;

public interface LogFileWriter {

	public final static String FILE_SUFFIX = ".txt";

	public abstract void writeToLogFile(final String text);

	public abstract void writeStacktracetoLogFile(Exception e);

	public abstract void close();

	public abstract String getFileName();

}