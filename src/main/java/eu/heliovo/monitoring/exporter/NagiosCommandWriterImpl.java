package eu.heliovo.monitoring.exporter;

import java.io.*;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Please see {@link NagiosCommandWriter} for a description.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class NagiosCommandWriterImpl implements NagiosCommandWriter {

	protected static final String COMMAND_ARGUMENT_DELIMITER = ";";
	private static final int TO_SECONDS_DIVISOR = 1000;

	private final File nagiosExternalCommandFile;
	private final boolean forceNagiosExternalCommandFileCreation;

	private final Logger logger = Logger.getLogger(getClass());

	@Autowired
	protected NagiosCommandWriterImpl(@Value("${nagiosExternalCommandFilePath}") String nagiosExternalCommandFile,
			@Value("${forceNagiosExternalCommandFileCreation}") boolean forceNagiosExternalCommandFileCreation) {

		this.nagiosExternalCommandFile = new File(nagiosExternalCommandFile);
		this.forceNagiosExternalCommandFileCreation = forceNagiosExternalCommandFileCreation;

		this.validateCommandFileAccess();
	}

	private void validateCommandFileAccess() {

		if (nagiosExternalCommandFile != null && !nagiosExternalCommandFile.exists()) {
			if (forceNagiosExternalCommandFileCreation) {
				logger.info("nagiosExternalCommandFile does not exist, but will be created");
				try {
					nagiosExternalCommandFile.createNewFile();
				} catch (IOException e) {
					throw new IllegalStateException("nagiosExternalCommandFile could not be created!", e);
				}
			} else {
				logger.warn("nagiosExternalCommandFile does not exist and is not allowed to be created!");
			}
		}

		if (nagiosExternalCommandFile == null) {
			throw new IllegalStateException("nagiosExternalCommandFile must not be null!");
		} else if (nagiosExternalCommandFile.exists() && !nagiosExternalCommandFile.canWrite()) {
			throw new IllegalStateException("nagiosExternalCommandFile cannot be written!");
		}
	}

	@Override
	public void write(NagiosCommand command, List<String> commandArguments) {
		long timeInSeconds = System.currentTimeMillis() / TO_SECONDS_DIVISOR;
		this.write(timeInSeconds, command, commandArguments);
	}

	protected void write(long timeInSeconds, NagiosCommand command, List<String> commandArguments) {

		if (nagiosExternalCommandFile.exists() || forceNagiosExternalCommandFileCreation) {
			String lineToWrite = buildLineToWrite(timeInSeconds, command, commandArguments);
			try {

				FileWriter fileWriter = new FileWriter(nagiosExternalCommandFile, true);
				try {
					fileWriter.write(lineToWrite);
				} finally {
					fileWriter.close();
				}

			} catch (IOException e) {
				throw new IllegalStateException("data could not be written!", e);
			}
		}
	}

	protected String buildLineToWrite(long timeInSeconds, NagiosCommand command, List<String> commandArguments) {

		StringBuffer buffer = new StringBuffer();

		buffer.append("[");
		buffer.append(timeInSeconds);
		buffer.append("]");
		buffer.append(" ");
		buffer.append(command.name());

		for (String commandArgument : commandArguments) {
			buffer.append(COMMAND_ARGUMENT_DELIMITER);
			buffer.append(commandArgument);
		}

		buffer.append("\n");

		return buffer.toString();
	}
}