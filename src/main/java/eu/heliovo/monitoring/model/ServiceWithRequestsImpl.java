package eu.heliovo.monitoring.model;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public class ServiceWithRequestsImpl implements ServiceWithRequests {

	private final String name;
	private final URL url;
	private final List<String> requests;

	protected ServiceWithRequestsImpl(final String name, final URL url, final List<String> requests) {
		this.name = name;
		this.url = url;
		this.requests = Collections.unmodifiableList(requests);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public URL getUrl() {
		return url;
	}

	@Override
	public List<String> getRequests() {
		return requests;
	}

}
