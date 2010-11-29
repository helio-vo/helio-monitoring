package eu.heliovo.monitoring.controller;

import junit.framework.Assert;

import org.junit.Test;

public class LogFilesDownloadControllerTest extends Assert {
	
	@Test
	public void testLogFilesDownloadController() throws Exception {

		// String logDir = System.getProperty("java.io.tmpdir");
		// if (!logDir.endsWith("/") && !logDir.endsWith("\\")) {
		// logDir = logDir + System.getProperty("file.separator");
		// }
		//
		// System.out.println(logDir);
		//
		// long currentTimeMilliseconds = System.currentTimeMillis();
		// String logFileName = "FooBarServer-" + currentTimeMilliseconds + LogFileWriter.FILE_SUFFIX;
		//
		// LogFileWriter logFileWriter = LoggingFactory.newLogFileWriter(logDir, logFileName);
		// logFileWriter.writeToLogFile("created");
		// logFileWriter.close();
		//
		// LogFilesDownloadController controller = new LogFilesDownloadController(logDir);
		//
		// MockHttpServletRequest request = new MockHttpServletRequest();
		// MockHttpServletResponse response = new MockHttpServletResponse();
		//
		// request.setProtocol("http");
		// request.setServerName("helio-dev.i4ds.technik.fhnw.ch");
		// request.setServerPort(8080);
		// request.setServletPath("helio-monitoring");
		// request.setRequestURI("http://helio-dev.i4ds.technik.fhnw.ch:8080/helio-monitoring/" + logFileName);
		// controller.downloadLogFile(request, response);
		//
		// System.out.println(response.getStatus());
	}
}