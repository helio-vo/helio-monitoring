package eu.heliovo.monitoring.logging;

/**
 * DummyLogFileWriter does nothing. Should be used as dummy, if an error occurs while creating the real LogFileWriter.
 * So that the rest of the code can be executed.
 * 
 * @author Kevin Seidler
 * 
 */
public final class DummyLogFileWriter implements LogFileWriter {

	private final String errorMessage;

	public DummyLogFileWriter(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public void write(final String text) {
	}

	@Override
	public void write(final Exception e) {
	}

	@Override
	public void close() {

	}

	@Override
	public String getFileName() {
		return "";
	}

	protected String getErrorMessage() {
		return errorMessage;
	}
}