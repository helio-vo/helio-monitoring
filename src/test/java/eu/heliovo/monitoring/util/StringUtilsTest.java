package eu.heliovo.monitoring.util;

import java.net.URL;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.*;

public class StringUtilsTest extends Assert {

	@Test
	public void testGetCanonicalName() throws Exception {
		
		Service service = ModelFactory.newService("", "MSSL ILS", new URL("http://www.helio-vo.eu"));
		assertEquals("MSSL_ILS", service.getCanonicalName());
		
		service = ModelFactory.newService("", "MSSL/ILS", new URL("http://www.helio-vo.eu"));
		assertEquals("MSSLILS", service.getCanonicalName());
		
		service = ModelFactory.newService("", "MSSL\\ /ILS", new URL("http://www.helio-vo.eu"));
		assertEquals("MSSL_ILS", service.getCanonicalName());
	}
}