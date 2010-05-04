package eu.heliovo.monitoring.logging;

public class DummyLogFileWriter implements LogFileWriter {

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
