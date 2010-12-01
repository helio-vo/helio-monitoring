package eu.heliovo.monitoring.daemon;

import junit.framework.Assert;

import org.junit.Test;

public class NagiosStatusTest extends Assert {

	@Test
	public void testNagiosStatus() {
		assertTrue(NagiosServiceStatus.OK.ordinal() == 0);
		assertTrue(NagiosServiceStatus.WARNING.ordinal() == 1);
		assertTrue(NagiosServiceStatus.CRITICAL.ordinal() == 2);
		assertTrue(NagiosServiceStatus.UNKNOWN.ordinal() == 3);
	}
}