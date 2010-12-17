package eu.heliovo.monitoring.exporter;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.test.util.TestServices;

public class NagiosServiceConfigUpdaterTest extends Assert {

	private static final File NAGIOS_SERVICE_CONFIG_DIR = new File(System.getProperty("java.io.tmpdir"));

	@Test
	public void testNagiosConfigServiceUpdater() {

		NagiosServiceConfigUpdater updater = new NagiosServiceConfigUpdater(NAGIOS_SERVICE_CONFIG_DIR.getPath());

		File[] oldConfigFiles = NAGIOS_SERVICE_CONFIG_DIR.listFiles();

		updater.updateServices(TestServices.LIST);

		File[] configFiles = NAGIOS_SERVICE_CONFIG_DIR.listFiles();
	}
}