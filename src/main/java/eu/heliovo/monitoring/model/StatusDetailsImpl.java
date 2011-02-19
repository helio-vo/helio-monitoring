package eu.heliovo.monitoring.model;

import static eu.heliovo.monitoring.util.ReflectionUtils.implementsInterface;

import java.net.URL;

import javax.xml.bind.annotation.*;
@SuppressWarnings("serial")
@XmlType(name = "StatusDetails")
@XmlAccessorType(XmlAccessType.FIELD)
public final class StatusDetailsImpl<MonitoredEntity> implements StatusDetails<MonitoredEntity> {

	@XmlTransient
	private final MonitoredEntity monitoredEntity;

	@SuppressWarnings("unused")
	@XmlElement
	private final String identifier; // only used the web service interface

	@XmlElement
	private final String name;

	@XmlElement
	private final URL url;

	@XmlElement
	private final Status status;

	@XmlElement
	private final long responseTimeInMillis;

	@XmlElement
	private final String message;

	@SuppressWarnings("unused")
	private StatusDetailsImpl() {
		throw new IllegalStateException("do not use this constructor, it is only needed for JAX-WS");
	}

	protected StatusDetailsImpl(MonitoredEntity monitoredEntity, String name, URL url, Status status,
			long responseTimeInMillis, String message) {

		this.name = name;
		this.url = url;
		this.status = status;
		this.responseTimeInMillis = responseTimeInMillis;
		this.message = message;
		this.monitoredEntity = monitoredEntity;

		this.identifier = getIdentifier(monitoredEntity);
	}

	private String getIdentifier(MonitoredEntity monitoredEntity) {

		if (monitoredEntity != null && implementsInterface(monitoredEntity, Service.class)) {
			Service service = (Service) monitoredEntity;
			return service.getIdentifier();
		} else {
			return this.name;
		}
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