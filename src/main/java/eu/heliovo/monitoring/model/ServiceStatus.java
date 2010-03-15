package eu.heliovo.monitoring.model;

import java.io.Serializable;

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
	private State state = State.DOWN;
	private int responseTime = 0; // response time in ms
	
	public ServiceStatus (final String id) {
		this.id = id;
	}
	
	public ServiceStatus(final String id, final State state, final int responseTime) {
		this.id = id;
		this.state = state;
		this.responseTime = responseTime;
	}

	public State getState() {
		return state;
	}

	public void setState(final State state) {
		this.state = state;
	}

	public int getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(final int responseTime) {
		this.responseTime = responseTime;
	}

	public String getId() {
		return id;
	}
}
