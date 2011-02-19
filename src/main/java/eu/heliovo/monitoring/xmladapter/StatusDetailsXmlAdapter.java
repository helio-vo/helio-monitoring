package eu.heliovo.monitoring.xmladapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import eu.heliovo.monitoring.model.*;

/**
 * This XML adapter maps the {@link StatusDetails} interface to its implementation {@link StatusDetailsImpl} for JAXB.
 * 
 * @author Kevin Seidler
 * 
 */
public class StatusDetailsXmlAdapter extends XmlAdapter<StatusDetailsImpl<?>, StatusDetails<?>> {

	@Override
	public StatusDetails<?> unmarshal(StatusDetailsImpl<?> implementation) throws Exception {
		return implementation;
	}

	@Override
	public StatusDetailsImpl<?> marshal(StatusDetails<?> interfaze) throws Exception {

		if (interfaze instanceof StatusDetailsImpl) {
			return (StatusDetailsImpl<?>) interfaze;
		} else {
			throw new IllegalStateException("the implementation of StatusDetails must be StatusDetailsImpl!");
		}
	}
}