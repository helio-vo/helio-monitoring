package eu.heliovo.monitoring.model;

import java.io.Serializable;
import java.net.URL;

/**
 * The service status made up by a identifier, the actual state (up or down) and
 * the response time if up.
 * 
 * @author Kevin Seidler
 * 
 */
@SuppressWarnings("serial")
public final class ServiceStatus implements Serializable {

	private final String id;
	private final URL url;
	private final State state;
	private final long responseTime; // response time in ms
	private final String message;
	
	public ServiceStatus(final String id, final URL url, final State state, final long responseTime,
			final String message) {
		this.id = id;
		this.url = url;
		this.state = state;
		this.responseTime = responseTime;
		this.message = message;
	}

	public State getState() {
		return state;
	}

	public long getResponseTime() {
		return responseTime;
	}

	public String getId() {
		return id;
	}

	public URL getUrl() {
		return url;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "ServiceStatus [id=" + id + ", message=" + message + ", responseTime=" + responseTime + ", state="
				+ state + ", url=" + url + "]";
	}
}
