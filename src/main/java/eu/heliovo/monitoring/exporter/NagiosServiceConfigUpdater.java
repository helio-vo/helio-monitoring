package eu.heliovo.monitoring.exporter;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import org.antlr.stringtemplate.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import eu.heliovo.monitoring.listener.ServiceUpdateListener;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.util.ServiceHostUtils;
import eu.heliovo.monitoring.validator.DirectoryAccessValidator;

/**
 * Nagios is using config files to define the presented hosts and services in its web interface. These files have to be
 * updated if the list of monitored services change. The NagiosConfigServiceUpdater provides this ability.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class NagiosServiceConfigUpdater implements ServiceUpdateListener {

	protected static final String CONFIG_RESOURCE_PATH = "/nagiosconfig/";
	protected static final String HOST_CONFIG_RESOURCE_PATH = CONFIG_RESOURCE_PATH + "hostconfig/";
	protected static final String MAIN_CONFIG_RESOURCE_PATH = CONFIG_RESOURCE_PATH + "nagios.cfg";
	protected static final String CONFIG_FILE_SUFFIX = ".cfg";
	private static final int TO_SECONDS_DIVISOR = 1000;
	protected static final String FAKE_HOST_ADDRESS = "123.104.65.40";

	protected final FilenameFilter configFilenameFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(CONFIG_FILE_SUFFIX);
		}
	};

	private final File nagiosServiceConfigDir;
	private final File nagiosMainConfig;
	private final Host monitoringServiceHost;
	private final NagiosCommandWriter nagiosCommandWriter;

	private final Logger logger = Logger.getLogger(this.getClass());

	// these filename have to be updated if changed under src/main/resources/nagiosconfig
	private final static String[] PREDEFINED_CONFIG_RESOURCE_FILENAMES = new String[] { "contacts_nagios2.cfg",
			"generic-host_nagios2.cfg", "generic-service_nagios2.cfg", "timeperiods_nagios2.cfg" };

	@Autowired
	public NagiosServiceConfigUpdater(@Value("${nagiosServiceConfigDir}") String nagiosServiceConfigDir,
			@Value("${nagiosMainConfig}") String nagiosMainConfig,
			@Value("${monitoringService.hostName}") String monitoringServiceHostName,
			NagiosCommandWriter nagiosCommandWriter) throws MalformedURLException {

		this.nagiosServiceConfigDir = new File(nagiosServiceConfigDir);
		this.nagiosMainConfig = new File(nagiosMainConfig);
		this.monitoringServiceHost = getMonitoringServiceHost(monitoringServiceHostName);
		this.nagiosCommandWriter = nagiosCommandWriter;

		DirectoryAccessValidator.validate(this.nagiosServiceConfigDir);
	}

	private Host getMonitoringServiceHost(String monitoringServiceHostName) throws MalformedURLException {

		URL monitoringServiceHostUrl = new URL("http://" + monitoringServiceHostName + "/");
		Set<Service> emptyServiceList = Collections.emptySet();
		return ModelFactory.newHost(monitoringServiceHostUrl, emptyServiceList);
	}

	@Override
	public void updateServices(Set<Service> newServices) {
		Set<Host> newHosts = ServiceHostUtils.getHostsFromServices(newServices);
		updateNagiosServiceConfigs(newHosts);
	}

	private void updateNagiosServiceConfigs(Set<Host> newHosts) {

		logger.debug("updating Nagios service configuration");

		// validating directory access rights on every call, because someone could have changed them while running
		try {
			DirectoryAccessValidator.validate(this.nagiosServiceConfigDir);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return;
		}

		StringTemplateGroup templateGroup = new StringTemplateGroup("nagiosConfigs");

		Map<Host, StringTemplate> hostConfigTemplates;
		hostConfigTemplates = fillAllHostConfigTemplates(newHosts, templateGroup);

		Map<Host, File> tempConfigFiles;
		try {
			tempConfigFiles = writeTemplatesToTemporaryFiles(hostConfigTemplates);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return;
		}

		deleteOldConfigFiles();

		try {
			copyTempFilesToNagiosConfigDir(tempConfigFiles);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return;
		}

		copyPredefinedConfigsToNagiosConfigDir();
		copyPredefinedMainConfig();

		deleteTempFiles(tempConfigFiles); // since the temp files have been copied, they can be deleted

		restartNagios();

		logger.debug("Nagios service configuration successfully updated");
	}

	protected Map<Host, StringTemplate> fillAllHostConfigTemplates(Set<Host> newHosts, StringTemplateGroup templateGroup) {

		Map<Host, StringTemplate> hostConfigTemplates = new HashMap<Host, StringTemplate>();
		for (Host host : newHosts) {

			StringTemplate hostTemplate = fillHostTemplate(templateGroup, host, "templates/nagiosHost");
			List<StringTemplate> serviceTemplates = fillServiceTemplates(templateGroup, host);
			StringTemplate hostConfigTemplate = fillHostConfigTemplate(templateGroup, hostTemplate, serviceTemplates);

			hostConfigTemplates.put(host, hostConfigTemplate);
		}

		// the monitorinServiceHost has a special Nagios host config
		StringTemplate monitoringServiceHostTemplate = fillHostTemplate(templateGroup, monitoringServiceHost,
				"templates/nagiosMonitoringServiceHostConfig");

		hostConfigTemplates.put(monitoringServiceHost, monitoringServiceHostTemplate);

		return hostConfigTemplates;
	}

	private StringTemplate fillHostTemplate(StringTemplateGroup templateGroup, Host host, String templatePath) {

		StringTemplate hostTemplate = templateGroup.getInstanceOf(templatePath);
		hostTemplate.setAttribute("hostName", host.getName());
		hostTemplate.setAttribute("hostAddress", getHostAdress(host));

		return hostTemplate;
	}

	// the IP address of the host does not matter, but must be provided in the Nagios host config
	// since an address like "127.0.0.1" is misleading in the GUI, using a fake address looks alright :)
	private String getHostAdress(Host host) {
		String hostAddress;
		try {
			hostAddress = host.getAddress();
		} catch (UnknownHostException e) {
			hostAddress = FAKE_HOST_ADDRESS;
			logger.error(e.getMessage(), e);
		}
		return hostAddress;
	}

	private List<StringTemplate> fillServiceTemplates(StringTemplateGroup templateGroup, Host host) {

		List<StringTemplate> serviceTemplates = new ArrayList<StringTemplate>();
		for (Service service : host.getServices()) {
			StringTemplate serviceTemplate = templateGroup.getInstanceOf("templates/nagiosService");
			serviceTemplate.setAttribute("hostName", host.getName());
			serviceTemplate.setAttribute("serviceName", service.getName());
			serviceTemplates.add(serviceTemplate);
		}

		return serviceTemplates;
	}

	private StringTemplate fillHostConfigTemplate(StringTemplateGroup templateGroup, StringTemplate hostTemplate,
			List<StringTemplate> serviceTemplates) {

		StringTemplate hostConfigTemplate = templateGroup.getInstanceOf("templates/nagiosHostConfig");
		hostConfigTemplate.setAttribute("hostTemplate", hostTemplate);
		hostConfigTemplate.setAttribute("serviceTemplates", serviceTemplates);
		return hostConfigTemplate;
	}

	protected Map<Host, File> writeTemplatesToTemporaryFiles(Map<Host, StringTemplate> hostConfigTemplates)
			throws IOException {

		Map<Host, File> tempConfigFiles = new HashMap<Host, File>();
		for (Entry<Host, StringTemplate> entry : hostConfigTemplates.entrySet()) {

			Host host = entry.getKey();
			StringTemplate hostConfigTemplate = entry.getValue();

			File tempConfigFile = File.createTempFile(host.getName(), CONFIG_FILE_SUFFIX);
			tempConfigFiles.put(host, tempConfigFile);

			byte[] hostConfigContent = hostConfigTemplate.toString().getBytes();
			FileCopyUtils.copy(hostConfigContent, tempConfigFile);
		}
		return tempConfigFiles;
	}

	protected void deleteOldConfigFiles() {
		for (File file : nagiosServiceConfigDir.listFiles(configFilenameFilter)) {
			if (!file.delete()) {
				logger.info("config file " + file.getName() + " could not be deleted");
			}
		}
	}

	protected void copyTempFilesToNagiosConfigDir(Map<Host, File> tempConfigFiles) throws IOException {

		for (Entry<Host, File> entry : tempConfigFiles.entrySet()) {

			Host host = entry.getKey();
			File hostConfigFile = entry.getValue();
			String destinationFileName = host.getName() + CONFIG_FILE_SUFFIX;

			File newConfigFile = new File(nagiosServiceConfigDir, destinationFileName);
			if (isWritable(newConfigFile)) {
				String filePath = newConfigFile.getPath();
				logger.error("config file " + filePath + " cannot be written, no write access, file skipped");
			} else {
				FileCopyUtils.copy(hostConfigFile, newConfigFile);
			}
		}
	}

	private boolean isWritable(File newConfigFile) {
		return newConfigFile.exists() && !newConfigFile.canWrite();
	}

	/**
	 * The predefined resource files cannot be listed and copied as files, because during build process they are put in
	 * a JAR file. Within a JAR file no standard file listing is possible. They have to be treated as resources. The
	 * direct resource file names are manually defined in
	 * {@link NagiosServiceConfigUpdater#PREDEFINED_CONFIG_RESOURCE_FILENAMES} and have to be maintained if the names
	 * change, files are added or deleted.
	 */
	protected void copyPredefinedConfigsToNagiosConfigDir() {

		for (String predefinedConfigFile : PREDEFINED_CONFIG_RESOURCE_FILENAMES) {
			try {
				File destinationFile = new File(nagiosServiceConfigDir, predefinedConfigFile);
				String predefinedConfigFileResourcePath = HOST_CONFIG_RESOURCE_PATH + predefinedConfigFile;

				InputStream configFile = this.getClass().getResourceAsStream(predefinedConfigFileResourcePath);
				FileCopyUtils.copy(configFile, new FileOutputStream(destinationFile));

			} catch (IOException e) {
				String nameOfFileToCopy = predefinedConfigFile;
				logger.error("predefined Nagios config file " + nameOfFileToCopy + " could not be copied!", e);
			}
		}
	}

	protected void copyPredefinedMainConfig() {

		if (isMainConfigWritable() || isMainConfigCreatable()) {
			InputStream source = this.getClass().getResourceAsStream(MAIN_CONFIG_RESOURCE_PATH);
			try {
				FileCopyUtils.copy(source, new FileOutputStream(nagiosMainConfig));
			} catch (IOException e) {
				String mainConfigName = nagiosMainConfig.getName();
				logger.error("predefined Nagios main config file " + mainConfigName + " could not be copied!", e);
			}
		} else {
			String mainConfigName = nagiosMainConfig.getName();
			logger.error("Nagios main config destination file " + mainConfigName
					+ " is not accessible or cannot be created!");
		}
	}

	private boolean isMainConfigCreatable() {
		return !nagiosMainConfig.exists() && nagiosMainConfig.getParentFile().exists()
				&& nagiosMainConfig.getParentFile().canWrite();
	}

	private boolean isMainConfigWritable() {
		return nagiosMainConfig.exists() && nagiosMainConfig.canWrite();
	}

	private void deleteTempFiles(Map<Host, File> tempConfigFiles) {
		for (File file : tempConfigFiles.values()) {
			if (!file.delete()) {
				logger.info("temporary file " + file.getName() + " could not be deleted");
			}
		}
	}

	private void restartNagios() {
		long currentTimeInMillis = System.currentTimeMillis() / TO_SECONDS_DIVISOR;
		List<String> restartArguments = new ArrayList<String>();
		restartArguments.add(String.valueOf(currentTimeInMillis));
		nagiosCommandWriter.write(NagiosCommand.RESTART_PROGRAM, restartArguments);
	}

	// TODO if a new host is added, send passive check for host is up, otherwise the host is shown as pending
	// see http://www.mail-archive.com/nagios-users@lists.sourceforge.net/msg15879.html
	// TODO (low prio) maybe send passive check, that host is down, if ping stage says, that host is down
}