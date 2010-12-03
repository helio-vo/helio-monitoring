package eu.heliovo.monitoring.serviceloader;

import java.util.List;

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
	public List<Service> loadServices() {
		return Services.LIST;
	}
}