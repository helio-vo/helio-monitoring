package eu.heliovo.monitoring.serviceloader;

import java.util.Set;

import eu.heliovo.monitoring.model.Service;

/**
 * Loads the Services to monitor from a source, e.g. a registry or a file.
 * 
 * @author Kevin Seidler
 * 
 */
public interface ServiceLoader {

	Set<Service> loadServices();
}