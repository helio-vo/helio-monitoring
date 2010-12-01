package eu.heliovo.monitoring.model;

import java.net.URL;

public final class ServiceStatusDetailsImpl implements ServiceStatusDetails {

	private final String id;
	private final URL url;
	private final Status status;
	private final long responseTimeInMillis;
	private final String message;

	public ServiceStatusDetailsImpl(String id, URL url, Status status, long responseTimeInMillis, String message) {

		this.id = id;
		this.url = url;
		this.status = status;
		this.responseTimeInMillis = responseTimeInMillis;
		this.message = message;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	@Override
	public long getResponseTimeInMillis() {
		return responseTimeInMillis;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public URL getUrl() {
		return url;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "ServiceStatusDetails [id=" + id + ", message=" + message + ", responseTimeInMillis="
				+ responseTimeInMillis + ", status=" + status + ", url=" + url + "]";
	}
}