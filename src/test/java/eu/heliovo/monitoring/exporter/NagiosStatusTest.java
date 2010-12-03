package eu.heliovo.monitoring.exporter;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.exporter.NagiosServiceStatus;

public class NagiosStatusTest extends Assert {

	@Test
	public void testNagiosStatus() {
		assertTrue(NagiosServiceStatus.OK.ordinal() == 0);
		assertTrue(NagiosServiceStatus.WARNING.ordinal() == 1);
		assertTrue(NagiosServiceStatus.CRITICAL.ordinal() == 2);
		assertTrue(NagiosServiceStatus.UNKNOWN.ordinal() == 3);
	}
}