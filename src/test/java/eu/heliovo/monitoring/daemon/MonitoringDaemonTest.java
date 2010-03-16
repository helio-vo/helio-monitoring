package eu.heliovo.monitoring.daemon;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.heliovo.monitoring.model.ServiceStatus;
import eu.heliovo.monitoring.model.State;

public class MonitoringDaemonTest extends Assert {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void testWriteToNagios() throws Exception {

		File nagiosExternalCommandFile;
		MonitoringDaemon daemon = new MonitoringDaemon("");

		long time = Long.MIN_VALUE;
		NagiosCommand command = null;
		String hostName = "";
		String serviceName = "";
		NagiosStatus status = NagiosStatus.WARNING;
		String statusMessage = "";

		/*
		 * test for null
		 */

		boolean illegalStateException = false;
		try {
			daemon.afterPropertiesSet();
			daemon.writeToNagiosExternalCommandFile(time, command, hostName, serviceName, status, statusMessage);
		} catch (final IllegalStateException e) {
			illegalStateException = true;
		}
		assertTrue(illegalStateException);

		/*
		 * test for file is not a file
		 */

		nagiosExternalCommandFile = new File("nagios");
		if (!nagiosExternalCommandFile.exists() && !nagiosExternalCommandFile.mkdir()) {
			throw new IllegalStateException("directory could not be created!");
		}
		daemon = new MonitoringDaemon("nagios");

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
		daemon = new MonitoringDaemon("nagios.cmd");

		illegalStateException = false;
		try {
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
		daemon = new MonitoringDaemon("nagios.cmd");

		time = System.currentTimeMillis() / 1000;
		command = NagiosCommand.PROCESS_SERVICE_CHECK_RESULT;
		hostName = "development";
		serviceName = "someService";
		status = NagiosStatus.OK;
		statusMessage = "someService is working as expected";

		daemon.writeToNagiosExternalCommandFile(time, command, hostName, serviceName, status, statusMessage);

		assertTrue(nagiosExternalCommandFile.exists());

		final List<String> lines = FileUtils.readLines(nagiosExternalCommandFile, MonitoringDaemon.FILE_ENCODING);

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
		final MonitoringDaemon daemon = new MonitoringDaemon("nagios2.cmd");

		final List<ServiceStatus> serviceStatus = new ArrayList<ServiceStatus>();

		final ServiceStatus first = new ServiceStatus("HEC", new URL(
				"http://helio.i4ds.technik.fhnw.ch:8080/core/HECService?wsdl"));
		first.setResponseTime(5);
		first.setState(State.UP);
		serviceStatus.add(first);

		final ServiceStatus second = new ServiceStatus("FrontendFacade", new URL(
				"http://helio.i4ds.technik.fhnw.ch:8080/core/FrontendFacadeService?wsdl"));
		second.setResponseTime(10);
		second.setState(State.DOWN);
		serviceStatus.add(second);

		final ServiceStatus third = new ServiceStatus("helio-dev WorkflowsService", new URL(
				"http://helio-dev.i4ds.technik.fhnw.ch/helio-wf/WorkflowsService?wsdl"));
		third.setResponseTime(15);
		third.setState(State.DOWN);
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
		String statusMessage = first.getState().name() + " response time = " + first.getResponseTime() + " ms";

		final String expectedFirstLine = buildCommandLine(time, hostName, "HEC", NagiosStatus.OK, statusMessage);

		logger.debug("expectedFirstLine: " + expectedFirstLine);
		logger.debug("actualFirstLine: " + actualFirstLine);

		assertEquals(expectedFirstLine, actualFirstLine);

		final String actualSecondLine = lines.get(1);

		time = System.currentTimeMillis() / 1000;
		hostName = "helio.i4ds.technik.fhnw.ch";
		statusMessage = second.getState().name() + " response time = " + second.getResponseTime() + " ms";

		final String expectedSecondLine = buildCommandLine(time, hostName, "FrontendFacade", NagiosStatus.CRITICAL,
				statusMessage);

		logger.debug("expectedSecondLine: " + expectedSecondLine);
		logger.debug("actualSecondLine: " + actualSecondLine);

		assertEquals(expectedSecondLine, actualSecondLine);
	}

	private final static String buildCommandLine(final long time, final String hostName, final String serviceName,
			final NagiosStatus status, final String statusMessage) {

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
