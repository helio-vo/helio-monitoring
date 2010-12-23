package eu.heliovo.monitoring.test.util;

import static eu.heliovo.monitoring.model.ModelFactory.newService;

import java.net.*;
import java.util.*;

import org.junit.Ignore;

import eu.heliovo.monitoring.model.Service;
import eu.heliovo.monitoring.statics.Services;

@Ignore
public final class TestServices {

	public static final Set<Service> LIST;

	private TestServices() {
	}

	static {
		try {
			Set<Service> services = new HashSet<Service>();
			services.addAll(Services.LIST);

			// just for testing purposes
			services.add(newService("FakeOfflineService", new URL("http://123.43.121.11/")));
			services.add(newService("NoWsdlOfflineService", new URL("http://helio-dev.i4ds.ch/fakeofflineservice")));

			LIST = Collections.unmodifiableSet(services);

		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
}