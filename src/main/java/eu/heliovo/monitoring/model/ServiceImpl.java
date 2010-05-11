package eu.heliovo.monitoring.model;

import java.net.URL;

public class ServiceImpl implements Service {

	private final String name;
	private final URL url;

	protected ServiceImpl(final String name, final URL url) {
		this.name = name;
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public URL getUrl() {
		return url;
	}

}
