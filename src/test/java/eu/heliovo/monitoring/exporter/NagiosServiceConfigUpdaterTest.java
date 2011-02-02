package eu.heliovo.monitoring.exporter;

import static eu.heliovo.monitoring.exporter.NagiosServiceConfigUpdater.CONFIG_FILE_SUFFIX;
import static eu.heliovo.monitoring.exporter.NagiosServiceConfigUpdater.HOST_CONFIG_RESOURCE_PATH;
import static eu.heliovo.monitoring.exporter.NagiosServiceConfigUpdater.MAIN_CONFIG_RESOURCE_PATH;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.antlr.stringtemplate.*;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;

import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.test.util.TestServices;
import eu.heliovo.monitoring.util.ServiceHostUtils;

public class NagiosServiceConfigUpdaterTest extends Assert {

	private static final String JAVA_TEMP_DIR = System.getProperty("java.io.tmpdir");
	private static final File NAGIOS_SERVICE_CONFIG_DIR = new File(JAVA_TEMP_DIR, "nagiosConfig");
	private static final File NAGIOS_MAIN_CONFIG = new File(JAVA_TEMP_DIR, "nagios.cfg");

	private final NagiosCommandWriter mockCommandWriter = new NagiosCommandWriter() {
		@Override
		public void write(NagiosCommand command, List<String> commandArguments) {
		}
	};

	public NagiosServiceConfigUpdaterTest() {
		cleanUp();
		if (!NAGIOS_SERVICE_CONFIG_DIR.exists()) {
			assertTrue(NAGIOS_SERVICE_CONFIG_DIR.mkdir());
		}
	}

	@Test
	public void testNagiosConfigServiceUpdater() throws Exception {

		NagiosServiceConfigUpdater updater;
		updater = new NagiosServiceConfigUpdater(NAGIOS_SERVICE_CONFIG_DIR.getPath(), NAGIOS_MAIN_CONFIG.getPath(),
				"www.i4ds.ch", mockCommandWriter);

		Set<Host> newHosts = ServiceHostUtils.getHostsFromServices(TestServices.LIST);

		StringTemplateGroup templateGroup = new StringTemplateGroup("nagiosConfigs");
		turnOffCachingForTesting(templateGroup);

		Map<Host, StringTemplate> filledTemplates = validateFilledTemplates(updater, newHosts, templateGroup);
		Map<Host, File> tempConfigFiles = validateTempConfigFiles(updater, filledTemplates);

		validateOldConfigFileDeletion(updater);

		validateCopiedConfigFiles(updater, tempConfigFiles);
		clearNagiosServiceConfigDir();

		validateConfigCopyWithExistingFile(updater, newHosts, filledTemplates, tempConfigFiles);
		clearNagiosServiceConfigDir();

		validateConfigCopyWithNonWritableFile(updater, newHosts, tempConfigFiles);

		updater.copyPredefinedConfigsToNagiosConfigDir();
		validatePredefinedConfigCopy(updater);

		updater.copyPredefinedMainConfig();
		validatePredefinedMainConfigCopy(updater);

		cleanUp();
	}

	private void validateOldConfigFileDeletion(NagiosServiceConfigUpdater updater) {
		updater.deleteOldConfigFiles();
		File[] existingConfigFiles = NAGIOS_SERVICE_CONFIG_DIR.listFiles(updater.configFilenameFilter);
		assertEquals(0, existingConfigFiles.length);
	}

	private void validatePredefinedConfigCopy(NagiosServiceConfigUpdater updater) {

		URL predefinedConfigResource = updater.getClass().getResource(HOST_CONFIG_RESOURCE_PATH);
		File predefinedConfigDir = new File(predefinedConfigResource.getFile());
		assertTrue(predefinedConfigDir.exists());

		List<File> existingNagiosConfigs = Arrays.asList(NAGIOS_SERVICE_CONFIG_DIR.listFiles());
		for (File predefinedConig : predefinedConfigDir.listFiles()) {
			assertTrue(existingNagiosConfigs.contains(new File(NAGIOS_SERVICE_CONFIG_DIR, predefinedConig.getName())));
		}
	}

	private void validateConfigCopyWithNonWritableFile(NagiosServiceConfigUpdater updater, Set<Host> newHosts,
			Map<Host, File> tempConfigFiles) throws IOException {

		Host hostFromNewHosts = selectAnyHost(newHosts);
		File fakeExistingConfigFile = createFakeExistingFile(hostFromNewHosts.getName());

		assertTrue(fakeExistingConfigFile.setReadOnly());

		// fakeExistingConfig cannot be overwritten, error message in console occurs
		updater.copyTempFilesToNagiosConfigDir(tempConfigFiles);

		fakeExistingConfigFile.setWritable(true);
	}

	private Host selectAnyHost(Set<Host> newHosts) {
		return newHosts.iterator().next();
	}

	private File createFakeExistingFile(String hostNameFromNewHosts) throws IOException {
		File fakeExistingConfigFile = new File(NAGIOS_SERVICE_CONFIG_DIR, hostNameFromNewHosts + CONFIG_FILE_SUFFIX);
		FileCopyUtils.copy("fakeConfigContent".getBytes(), fakeExistingConfigFile);
		return fakeExistingConfigFile;
	}

	private void validateConfigCopyWithExistingFile(NagiosServiceConfigUpdater updater, Set<Host> newHosts,
			Map<Host, StringTemplate> filledTemplates, Map<Host, File> tempConfigFiles) throws IOException,
			FileNotFoundException {

		Host hostFromNewHosts = selectAnyHost(newHosts);
		File fakeExistingConfigFile = createFakeExistingFile(hostFromNewHosts.getName());

		// fakeExistingConfig file should now be overwritten, because its name is one of the new hosts
		updater.copyTempFilesToNagiosConfigDir(tempConfigFiles);

		String actualFileContent = FileCopyUtils.copyToString(new FileReader(fakeExistingConfigFile));
		String expectedFileContent = filledTemplates.get(hostFromNewHosts).toString();
		assertEquals(expectedFileContent, actualFileContent);
	}

	private void validateCopiedConfigFiles(NagiosServiceConfigUpdater updater, Map<Host, File> tempConfigFiles)
			throws IOException, FileNotFoundException {

		File[] oldConfigFiles = NAGIOS_SERVICE_CONFIG_DIR.listFiles();
		// config dir should be empty here, because of the cleanup of the last run
		assertEquals(0, oldConfigFiles.length);

		updater.copyTempFilesToNagiosConfigDir(tempConfigFiles);

		List<File> newConfigFiles = Arrays.asList(NAGIOS_SERVICE_CONFIG_DIR.listFiles());
		assertEquals(tempConfigFiles.size(), newConfigFiles.size());

		for (Entry<Host, File> entry : tempConfigFiles.entrySet()) {

			Host host = entry.getKey();
			File tempTemplateFile = entry.getValue();

			String expectedFileName = host.getName() + CONFIG_FILE_SUFFIX;
			File expectedFile = new File(NAGIOS_SERVICE_CONFIG_DIR, expectedFileName);

			assertTrue(newConfigFiles.contains(expectedFile));

			String expectedFileContent = FileCopyUtils.copyToString(new FileReader(tempTemplateFile));
			String actualFileContent = FileCopyUtils.copyToString(new FileReader(expectedFile));

			assertEquals(expectedFileContent, actualFileContent);
		}
	}

	private void clearNagiosServiceConfigDir() {
		File[] filesToDelete = NAGIOS_SERVICE_CONFIG_DIR.listFiles();
		for (File file : filesToDelete) {
			assertTrue(file.setWritable(true));
			assertTrue(file.delete());
		}
	}

	private Map<Host, File> validateTempConfigFiles(NagiosServiceConfigUpdater updater,
			Map<Host, StringTemplate> filledTemplates) throws IOException, FileNotFoundException {

		Map<Host, File> tempConfigFiles = updater.writeTemplatesToTemporaryFiles(filledTemplates);
		for (Entry<Host, File> entry : tempConfigFiles.entrySet()) {

			Host host = entry.getKey();
			File tempTemplateFile = entry.getValue();

			assertTrue(tempTemplateFile.getName().startsWith(host.getName()));
			assertTrue(tempTemplateFile.getName().endsWith(CONFIG_FILE_SUFFIX));

			String tempConfigFileContent = FileCopyUtils.copyToString(new FileReader(tempTemplateFile));
			String filledTemplateContent = filledTemplates.get(host).toString();

			assertEquals(filledTemplateContent, tempConfigFileContent);
		}
		return tempConfigFiles;
	}

	private Map<Host, StringTemplate> validateFilledTemplates(NagiosServiceConfigUpdater updater, Set<Host> newHosts,
			StringTemplateGroup templateGroup) {

		Map<Host, StringTemplate> filledTemplates = updater.fillAllHostConfigTemplates(newHosts, templateGroup);
		for (Entry<Host, StringTemplate> entry : filledTemplates.entrySet()) {

			Host host = entry.getKey();
			String template = entry.getValue().toString();

			assertTrue(template.contains(host.getName()));
			try {
				assertTrue(template.contains(host.getAddress()));
			} catch (UnknownHostException e) {
				assertTrue(template.contains(NagiosServiceConfigUpdater.FAKE_HOST_ADDRESS));
				e.printStackTrace();
			}

			for (Service service : host.getServices()) {
				assertTrue(template.contains(service.getName() + " -ping-"));
				assertTrue(template.contains(service.getName() + " -method call-"));
				assertTrue(template.contains(service.getName() + " -testing-"));
			}

			System.out.println(host.getName() + " template:");
			System.out.println(template);
		}
		return filledTemplates;
	}

	private void turnOffCachingForTesting(StringTemplateGroup templateGroup) {
		templateGroup.setRefreshInterval(0); // no caching
		templateGroup.setRefreshInterval(Integer.MAX_VALUE); // no refreshing
	}

	private void validatePredefinedMainConfigCopy(NagiosServiceConfigUpdater updater) throws IOException,
			FileNotFoundException {

		URL mainConfigResource = updater.getClass().getResource(MAIN_CONFIG_RESOURCE_PATH);
		File mainConfig = new File(mainConfigResource.getFile());
		assertTrue(mainConfig.exists());
		assertTrue(NAGIOS_MAIN_CONFIG.exists());
		String sourceMainConfigContent = FileCopyUtils.copyToString(new FileReader(mainConfig));
		String destinationMainConfigContent = FileCopyUtils.copyToString(new FileReader(NAGIOS_MAIN_CONFIG));
		assertEquals(sourceMainConfigContent, destinationMainConfigContent);
	}

	private void cleanUp() {
		if (NAGIOS_SERVICE_CONFIG_DIR.exists()) {
			clearNagiosServiceConfigDir();
			assertTrue(NAGIOS_SERVICE_CONFIG_DIR.delete());
		}
		if (NAGIOS_MAIN_CONFIG.exists()) {
			assertTrue(NAGIOS_MAIN_CONFIG.delete());
		}
	}
}