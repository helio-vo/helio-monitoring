package eu.heliovo.monitoring.logging;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class LogFileWriterTest {

	private final LoggingFactory loggingFactory = LoggingTestUtils.getLoggingFactory();

	@Test
	public void testLogFileWriter() throws Exception {

		LogFileWriter logFileWriter = loggingFactory.newLogFileWriter("HEC");
		logFileWriter.write("sometext");
		logFileWriter.write("someOtherText");
		logFileWriter.close();

		String javaTempDir = System.getProperty("java.io.tmpdir");
		File logFile = new File(javaTempDir, logFileWriter.getFileName());

		System.out.println("Log file: " + logFile);
		System.out.println();

		String logFileContent = FileUtils.readFileToString(logFile);

		System.out.println("Log file content: ");
		System.out.println(logFileContent);

		assertTrue(logFileContent.contains("sometext"));
		assertTrue(logFileContent.contains("someOtherText"));
	}
}