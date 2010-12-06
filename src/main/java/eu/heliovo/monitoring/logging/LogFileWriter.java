package eu.heliovo.monitoring.logging;

/**
 * Writes logs in a file.
 * 
 * @author Kevin Seidler
 * 
 */
public interface LogFileWriter {

	String FILE_SUFFIX = ".txt";

	/**
	 * Writes a given text to the log file.
	 * 
	 * @param text
	 */
	void write(String text);

	/**
	 * Writes an exeception with stack trace to the log file.
	 * 
	 * @param e the exception
	 */
	void write(Exception e);

	/**
	 * Closed the underlying file.
	 */
	void close();

	/**
	 * Returns file name of the log file
	 * 
	 * @return file name of the log file
	 */
	String getFileName();
}