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
public class ServiceStatus implements Serializable {

	private final String id;
	private final URL url;
	private State state = State.DOWN;
	private long responseTime = 0; // response time in ms
	private String message = "";
	
	public ServiceStatus(final String id, final URL url) {
		this.id = id;
		this.url = url;
	}
	
	public ServiceStatus(final String id, final URL url, final State state, final long responseTime) {
		this.id = id;
		this.url = url;
		this.state = state;
		this.responseTime = responseTime;
	}

	public State getState() {
		return state;
	}

	public void setState(final State state) {
		this.state = state;
	}

	public long getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(final long responseTime) {
		this.responseTime = responseTime;
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

	public void setMessage(final String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "ServiceStatus [id=" + id + ", message=" + message + ", responseTime=" + responseTime + ", state="
				+ state + ", url=" + url + "]";
	}
}
