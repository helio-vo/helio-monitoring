package eu.heliovo.monitoring.model;

import java.io.Serializable;
import java.net.URL;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import eu.heliovo.monitoring.xmladapter.StatusDetailsXmlAdapter;

/**
 * Status details are made up by the monitored entity (a host or a service), a name (for the entity), a URL to that
 * monitored entitiy, the actual status (e.g. OK or CRITICAL), the response time if OK and a status message.
 * 
 * @author Kevin Seidler
 * 
 */
@XmlJavaTypeAdapter(value = StatusDetailsXmlAdapter.class)
public interface StatusDetails<MonitoredEntity> extends Serializable {

	MonitoredEntity getMonitoredEntity();

	String getName();

	URL getUrl();

	Status getStatus();

	long getResponseTimeInMillis();

	String getMessage();
}