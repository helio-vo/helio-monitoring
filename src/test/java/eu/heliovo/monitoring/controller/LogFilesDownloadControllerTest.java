package eu.heliovo.monitoring.controller;

import java.io.*;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.mock.web.*;
import org.springframework.util.FileCopyUtils;

import eu.heliovo.monitoring.logging.*;

public class LogFilesDownloadControllerTest extends Assert {

	private final String logDir = System.getProperty("java.io.tmpdir");

	@Test
	public void testDownloadLogFileWithExistingFile() throws Exception {

		String logFileName = createLogFileForTesting();

		LogFilesDownloadController controller = new LogFilesDownloadController(logDir);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setServletPath("/" + logFileName);
		controller.downloadLogFile(request, response);

		assertEquals(200, response.getStatus());

		byte[] responseContent = response.getContentAsByteArray();
		File responseOutputFile = writeResponseContentToTempFile(responseContent);
		FileReader fileReader = new FileReader(responseOutputFile);

		char[] responseOutputFileContent = new char[1000];
		
		int bytesRead = fileReader.read(responseOutputFileContent);
		assertEquals(responseContent.length, bytesRead);
		
		String responseOutputFileContentAsString = String.valueOf(responseOutputFileContent);
		System.out.println(responseOutputFileContentAsString);
		assertTrue(responseOutputFileContentAsString.contains("created"));

		fileReader.close();
	}

	private File writeResponseContentToTempFile(byte[] responseContent) throws IOException {
		File responseOutputFile = File.createTempFile("responseOutput", ".txt", new File(logDir));
		responseOutputFile.deleteOnExit();

		FileCopyUtils.copy(responseContent, responseOutputFile);
		return responseOutputFile;
	}

	private String createLogFileForTesting() {

		LogFileWriter logFileWriter = LoggingTestUtils.getLoggingFactory().newLogFileWriter("FooBarService");
		logFileWriter.write("created");

		String logFileName = logFileWriter.getFileName();

		logFileWriter.close();
		return logFileName;
	}

	@Test
	public void testDownloadLogFileWithNotExistingFile() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		LogFilesDownloadController controller = new LogFilesDownloadController(logDir);

		request.setServletPath("/FileDoesNotExist.txt");
		controller.downloadLogFile(request, response);

		assertEquals(404, response.getStatus());
	}
}