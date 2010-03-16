package eu.heliovo.monitoring.daemon;

import junit.framework.Assert;

import org.junit.Test;

public class NagiosStatusTest extends Assert {

	@Test
	public void testNagiosStatus() {
		assertTrue(NagiosStatus.OK.ordinal() == 0);
		assertTrue(NagiosStatus.WARNING.ordinal() == 1);
		assertTrue(NagiosStatus.CRITICAL.ordinal() == 2);
	}

}
