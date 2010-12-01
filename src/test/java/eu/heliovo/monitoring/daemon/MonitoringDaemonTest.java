package eu.heliovo.monitoring.daemon;

import static eu.heliovo.monitoring.model.ServiceFactory.newServiceStatusDetails;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Test;

import eu.heliovo.monitoring.model.ServiceStatusDetails;
import eu.heliovo.monitoring.model.Status;

public class MonitoringDaemonTest extends Assert {

	private final Logger logger = Logger.getLogger(this.getClass());

	@Test
	public void testWriteToNagios() throws Exception {

		long time = Long.MIN_VALUE;
		NagiosCommand command = NagiosCommand.PROCESS_SERVICE_CHECK_RESULT;
		String hostName = "";
		String serviceName = "";
		NagiosServiceStatus status = NagiosServiceStatus.WARNING;
		String statusMessage = "";

		/*
		 * test for failing file creation
		 */

		boolean illegalStateException = false;
		try {
			MonitoringDaemon daemon = new MonitoringDaemon("", true);
			daemon.writeToNagiosExternalCommandFile(time, command, hostName, serviceName, status, statusMessage);
		} catch (final IllegalStateException e) {
			illegalStateException = true;
		}
		assertTrue(illegalStateException);

		/*
		 * test for forceNagiosExternalCommandFileCreation = false
		 */

		MonitoringDaemon daemon = new MonitoringDaemon("nagios", false);
		// just a warning log message that this file does not exist and is not allowed to be created

		/*
		 * test for file is not a file
		 */

		File nagiosExternalCommandFile = new File("nagios");
		if (!nagiosExternalCommandFile.exists() && !nagiosExternalCommandFile.mkdir()) {
			throw new IllegalStateException("directory could not be created!");
		}
		daemon = new MonitoringDaemon("nagios", true);

		illegalStateException = false;
		try {
			daemon.writeToNagiosExternalCommandFile(time, command, hostName, serviceName, status, statusMessage);
		} catch (final IllegalStateException e) {
			illegalStateException = true;
		}
		assertTrue(illegalStateException);

		/*
		 * test for canWrite?
		 */

		nagiosExternalCommandFile = new File("nagios.cmd");
		if (!nagiosExternalCommandFile.exists() && !nagiosExternalCommandFile.createNewFile()) {
			throw new IllegalStateException("nagiosExternalCommandFile could not be created!");
		}
		if (!nagiosExternalCommandFile.setReadOnly()) {
			throw new IllegalStateException("readOnly cannot be set!");
		}
		// nagiosExternalCommandFile is now readOnly

		illegalStateException = false;
		try {
			daemon = new MonitoringDaemon(nagiosExternalCommandFile.getPath(), true);
			daemon.writeToNagiosExternalCommandFile(time, command, hostName, serviceName, status, statusMessage);
		} catch (final IllegalStateException e) {
			illegalStateException = true;
		}
		assertTrue(illegalStateException);

		/*
		 * test for written correctly
		 */

		if (nagiosExternalCommandFile.exists() && !nagiosExternalCommandFile.delete()) {
			throw new IllegalStateException("file nagios.cmd could not be deleted!");
		}

		nagiosExternalCommandFile = new File("nagios.cmd");
		daemon = new MonitoringDaemon("nagios.cmd", true);

		time = System.currentTimeMillis() / 1000;
		command = NagiosCommand.PROCESS_SERVICE_CHECK_RESULT;
		hostName = "development";
		serviceName = "someService";
		status = NagiosServiceStatus.OK;
		statusMessage = "someService is working as expected";

		daemon.writeToNagiosExternalCommandFile(time, command, hostName, serviceName, status, statusMessage);

		assertTrue(nagiosExternalCommandFile.exists());

		List<String> lines = FileUtils.readLines(nagiosExternalCommandFile, MonitoringDaemon.FILE_ENCODING);

		assertNotNull(lines);
		assertTrue(lines.size() > 0);

		final String actualLastLine = lines.get(lines.size() - 1);

		final String expectedLastLine = buildCommandLine(time, hostName, serviceName, status, statusMessage);

		logger.debug("expectedLastLine: " + expectedLastLine);
		logger.debug("actualLastLine: " + actualLastLine);

		assertEquals(expectedLastLine, actualLastLine);

		// TODO test null Parameters etc.
	}

	@Test
	public void testWriteServiceStatusToNagios() throws Exception {

		final File nagiosExternalCommandFile = new File("nagios2.cmd");
		final RemotingMonitoringDaemon daemon = new MonitoringDaemon("nagios2.cmd", true);

		final List<ServiceStatusDetails> serviceStatus = new ArrayList<ServiceStatusDetails>();

		final String message = Status.OK.name() + " - response time = " + 5 + " ms";
		final String firstUrl = "http://helio.i4ds.technik.fhnw.ch:8080/core/HECService?wsdl";
		final ServiceStatusDetails first = newServiceStatusDetails("HEC", new URL(firstUrl), Status.OK, 5, message);
		serviceStatus.add(first);

		final ServiceStatusDetails second = newServiceStatusDetails("FrontendFacade", new URL(
				"http://helio.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl"), Status.CRITICAL, 10,
				Status.CRITICAL.name() + " - response time = " + 10 + " ms");
		serviceStatus.add(second);

		final ServiceStatusDetails third = newServiceStatusDetails("helio-dev WorkflowsService", new URL(
				"http://helio-dev.i4ds.technik.fhnw.ch/helio-wf/WorkflowsService?wsdl"), Status.CRITICAL, 15,
				Status.CRITICAL.name() + " - response time = " + 15 + " ms");
		serviceStatus.add(third);

		assertTrue(serviceStatus.size() == 3);

		daemon.writeServiceStatusToNagios(serviceStatus);

		final List<String> lines = FileUtils.readLines(nagiosExternalCommandFile, MonitoringDaemon.FILE_ENCODING);

		assertTrue(lines.size() == 3);

		logger.debug("=== lines: ===");
		for (final String line : lines) {
			logger.debug(line);
		}
		logger.debug("=== end lines ===");

		final String actualFirstLine = lines.get(0);

		long time = System.currentTimeMillis() / 1000;
		String hostName = "helio.i4ds.technik.fhnw.ch";
		String statusMessage = first.getStatus().name() + " - response time = " + first.getResponseTimeInMillis() + " ms";

		final String expectedFirstLine = buildCommandLine(time, hostName, "HEC", NagiosServiceStatus.OK, statusMessage);

		logger.debug("expectedFirstLine: " + expectedFirstLine);
		logger.debug("actualFirstLine: " + actualFirstLine);

		assertEquals(expectedFirstLine, actualFirstLine);

		final String actualSecondLine = lines.get(1);

		time = System.currentTimeMillis() / 1000;
		hostName = "helio.i4ds.technik.fhnw.ch";
		statusMessage = second.getStatus().name() + " - response time = " + second.getResponseTimeInMillis() + " ms";

		final String expectedSecondLine = buildCommandLine(time, hostName, "FrontendFacade", NagiosServiceStatus.CRITICAL,
				statusMessage);

		logger.debug("expectedSecondLine: " + expectedSecondLine);
		logger.debug("actualSecondLine: " + actualSecondLine);

		assertEquals(expectedSecondLine, actualSecondLine);
	}

	private final static String buildCommandLine(final long time, final String hostName, final String serviceName,
			final NagiosServiceStatus status, final String statusMessage) {

		// building nagios command line, e.g.: [1268669735]
		// PROCESS_SERVICE_CHECK_RESULT;development;HECWebService.sql;1;Hello
		// World!

		final StringBuffer buffer = new StringBuffer("[");
		buffer.append(time);
		buffer.append("]");
		buffer.append(" ");
		buffer.append(NagiosCommand.PROCESS_SERVICE_CHECK_RESULT.name());
		buffer.append(";");
		buffer.append(hostName);
		buffer.append(";");
		buffer.append(serviceName);
		buffer.append(";");
		buffer.append(status.ordinal());
		buffer.append(";");
		buffer.append(statusMessage);

		return buffer.toString();
	}

	@AfterClass
	public static void cleanUp() {
		if (new File("nagios.cmd").exists() && !new File("nagios.cmd").delete()) {
			throw new IllegalStateException("file nagios.cmd could not be deleted!");
		}
		if (new File("nagios2.cmd").exists() && !new File("nagios2.cmd").delete()) {
			throw new IllegalStateException("file nagios.cmd could not be deleted!");
		}
		if (new File("nagios").exists() && !new File("nagios").delete()) {
			throw new IllegalStateException("directory nagios could not be deleted!");
		}
	}
}
