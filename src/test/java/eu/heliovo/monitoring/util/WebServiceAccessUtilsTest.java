package eu.heliovo.monitoring.util;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.util.WebServiceAccessUtils;

public class WebServiceAccessUtilsTest extends Assert {

	@Test
	public void testWebServiceAccessUtil() throws Exception {
		new WebServiceAccessUtils().getAccess();
	}
}
