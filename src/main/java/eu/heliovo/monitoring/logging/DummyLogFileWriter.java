package eu.heliovo.monitoring.logging;

/**
 * DummyLogFileWriter does nothing. Should be used as dummy, if an error occurs while creating the real LogFileWriter.
 * So that the rest of the code can be executed.
 * 
 * @author Kevin Seidler
 * 
 */
public final class DummyLogFileWriter implements LogFileWriter {

	@Override
	public void writeToLogFile(final String text) {
	}

	@Override
	public void writeStacktracetoLogFile(final Exception e) {
	}

	@Override
	public void close() {

	}

	@Override
	public String getFileName() {
		return "";
	}

}
