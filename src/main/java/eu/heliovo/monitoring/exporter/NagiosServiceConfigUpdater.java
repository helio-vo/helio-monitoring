package eu.heliovo.monitoring.exporter;

import java.io.File;
import java.util.List;

import org.antlr.stringtemplate.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import eu.heliovo.monitoring.listener.ServiceUpdateListener;
import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.validator.DirectoryAccessValidator;

/**
 * Nagios is using config files to define the presented hosts and services in its web interface. These files have to be
 * updated if the list of monitored services change. The NagiosConfigServiceUpdater provides this ability.
 * 
 * @author Kevin Seidler
 * 
 */
public final class NagiosServiceConfigUpdater implements ServiceUpdateListener {

	private final File nagiosServiceConfigDir;

	private final Logger logger = Logger.getLogger(this.getClass());

	public NagiosServiceConfigUpdater(@Value("${nagiosServiceConfigDir}") String nagiosServiceConfigDir) {

		this.nagiosServiceConfigDir = new File(nagiosServiceConfigDir);
		DirectoryAccessValidator.validate(this.nagiosServiceConfigDir);
	}

	@Override
	public void updateServices(List<Service> newServices) {
		updateNagiosServiceConfigs();
	}

	private void updateNagiosServiceConfigs() {

		// Look for templates in CLASSPATH as resources
		StringTemplateGroup templateGroup = new StringTemplateGroup("nagiosConfigs");

		// turn caching of for testing
		templateGroup.setRefreshInterval(0); // no caching
		templateGroup.setRefreshInterval(Integer.MAX_VALUE); // no refreshing

		StringTemplate nagiosHostConfigTemplate = templateGroup.getInstanceOf("templates/nagioshostconfig");

	}
}