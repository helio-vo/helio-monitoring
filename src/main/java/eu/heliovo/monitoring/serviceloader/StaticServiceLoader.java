package eu.heliovo.monitoring.serviceloader;

import java.util.Set;

import org.springframework.stereotype.Component;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.statics.Services;

/**
 * Returns a static service list manually maintained.
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class StaticServiceLoader implements ServiceLoader {

	@Override
	public Set<Service> loadServices() {
		return Services.LIST;
	}
}