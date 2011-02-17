package eu.heliovo.monitoring.exporter;

import java.io.*;
import java.util.*;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.*;

// TODO use java temp dir instead of creating local files
public class NagiosCommandWriterTest extends Assert {

	@Test
	public void testWriteToNagios() throws Exception {

		long time = Long.MIN_VALUE;
		NagiosCommand command = NagiosCommand.PROCESS_SERVICE_CHECK_RESULT;
		String hostName = "";
		String serviceName = "";
		String status = String.valueOf(NagiosServiceStatus.WARNING.ordinal());
		String statusMessage = "";
		List<String> commandArguments = Arrays.asList(hostName, serviceName, status, statusMessage);

		testFailingFileCreation(time, command, commandArguments);
		testDontForceFileCreation();
		testForFileIsNotAFile(time, command, commandArguments);
		testNotWritable(time, command, commandArguments);
		testCommandWrittenCorrectly();
	}

	private void testCommandWrittenCorrectly() throws IOException {

		File nagiosExternalCommandFile = new File("nagios.cmd");
		NagiosCommandWriterImpl commandWriter = new NagiosCommandWriterImpl("nagios.cmd", true);

		long time = System.currentTimeMillis() / 1000;
		NagiosCommand command = NagiosCommand.PROCESS_SERVICE_CHECK_RESULT;
		String hostName = "development";
		String serviceName = "someService";
		String status = String.valueOf(NagiosServiceStatus.OK.ordinal());
		String statusMessage = "someService is working as expected";
		List<String> commandArguments = Arrays.asList(hostName, serviceName, status, statusMessage);

		commandWriter.write(time, command, commandArguments);

		assertTrue(nagiosExternalCommandFile.exists());

		List<String> lines = FileUtils.readLines(nagiosExternalCommandFile);

		assertNotNull(lines);
		assertTrue(lines.size() > 0);

		String actualLastLine = lines.get(lines.size() - 1);
		String expectedLastLine = buildCommandLine(time, commandArguments);

		System.out.println("expectedLastLine: " + expectedLastLine);
		System.out.println("actualLastLine: " + actualLastLine);

		assertEquals(expectedLastLine, actualLastLine);
	}

	private void testNotWritable(long time, NagiosCommand command, List<String> commandArguments) throws IOException {

		File nagiosExternalCommandFile = new File("nagios.cmd");
		if (!nagiosExternalCommandFile.exists() && !nagiosExternalCommandFile.createNewFile()) {
			throw new IllegalStateException("nagiosExternalCommandFile could not be created!");
		}
		if (!nagiosExternalCommandFile.setReadOnly()) {
			throw new IllegalStateException("readOnly cannot be set!");
		}
		// nagiosExternalCommandFile is now readOnly

		boolean illegalStateException = false;
		try {
			NagiosCommandWriterImpl commandWriter = new NagiosCommandWriterImpl(nagiosExternalCommandFile.getPath(),
					true);
			commandWriter.write(time, command, commandArguments);
		} catch (final IllegalStateException e) {
			illegalStateException = true;
		}
		assertTrue(illegalStateException);

		// test for written correctly
		if (nagiosExternalCommandFile.exists() && !nagiosExternalCommandFile.delete()) {
			throw new IllegalStateException("file nagios.cmd could not be deleted!");
		}
	}

	private void testForFileIsNotAFile(long time, NagiosCommand command, List<String> commandArguments) {
		File nagiosExternalCommandFile = new File("nagios");
		if (!nagiosExternalCommandFile.exists() && !nagiosExternalCommandFile.mkdir()) {
			throw new IllegalStateException("directory could not be created!");
		}
		NagiosCommandWriterImpl commandWriter = new NagiosCommandWriterImpl("nagios", true);

		boolean illegalStateException = false;
		try {
			commandWriter.write(time, command, commandArguments);
		} catch (final IllegalStateException e) {
			illegalStateException = true;
		}
		assertTrue(illegalStateException);
	}

	private void testDontForceFileCreation() {
		new NagiosCommandWriterImpl("nagios", false);
		// just a warning log message that this file does not exist and is not allowed to be created
	}

	private void testFailingFileCreation(long time, NagiosCommand command, List<String> commandArguments) {

		boolean illegalStateException = false;
		try {
			NagiosCommandWriterImpl commandWriter = new NagiosCommandWriterImpl("", true);

			commandWriter.write(time, command, commandArguments);
		} catch (IllegalStateException e) {
			illegalStateException = true;
		}
		assertTrue(illegalStateException);
	}

	private final static String buildCommandLine(final long time, List<String> commandArguments) {

		// building nagios command line, e.g.: [1268669735]
		// PROCESS_SERVICE_CHECK_RESULT;development;HECWebService.sql;1;Hello World!

		StringBuffer buffer = new StringBuffer("[");
		buffer.append(time);
		buffer.append("]");
		buffer.append(" ");
		buffer.append(NagiosCommand.PROCESS_SERVICE_CHECK_RESULT.name());

		for (String commandArgument : commandArguments) {
			buffer.append(NagiosCommandWriterImpl.COMMAND_ARGUMENT_DELIMITER);
			buffer.append(commandArgument);
		}

		return buffer.toString();
	}

	@AfterClass
	public static void cleanUp() {
		if (new File("nagios.cmd").exists() && !new File("nagios.cmd").delete()) {
			throw new IllegalStateException("file nagios.cmd could not be deleted!");
		}
		if (new File("nagios").exists() && !new File("nagios").delete()) {
			throw new IllegalStateException("directory nagios could not be deleted!");
		}
	}
}