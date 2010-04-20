package eu.heliovo.monitoring.model;

import java.net.URL;

public class Service {

	private final String name;
	private final URL url;

	public Service(final String name, final URL url) {
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
