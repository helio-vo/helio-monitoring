package eu.heliovo.monitoring.model;

import java.net.URL;

@SuppressWarnings("serial")
public final class StatusDetailsImpl<MonitoredEntity> implements StatusDetails<MonitoredEntity> {

	private final MonitoredEntity monitoredEntity;
	private final String name;
	private final URL url;
	private final Status status;
	private final long responseTimeInMillis;
	private final String message;

	protected StatusDetailsImpl(MonitoredEntity monitoredEntity, String name, URL url, Status status,
			long responseTimeInMillis, String message) {

		this.name = name;
		this.url = url;
		this.status = status;
		this.responseTimeInMillis = responseTimeInMillis;
		this.message = message;
		this.monitoredEntity = monitoredEntity;
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
	public String getName() {
		return name;
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
		return "ServiceStatusDetails [name=" + name + ", message=" + message + ", responseTimeInMillis="
				+ responseTimeInMillis + ", status=" + status + ", url=" + url + "]";
	}

	@Override
	public MonitoredEntity getMonitoredEntity() {
		return monitoredEntity;
	}
}