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

	@Test
	public void testReplaceUrlAsHtmlLink() throws Exception {

		String text = "text	http://www.helio-vo.eu	text http://www.google.ch text";
		String replacedText = StringUtils.replaceUrlAsHtmlAnchor(text);

		System.out.println(replacedText);

		StringBuffer expected = new StringBuffer();
		expected.append("text <a href=\"http://www.helio-vo.eu\" target=\"_blank\">http://www.helio-vo.eu</a> text ");
		expected.append("<a href=\"http://www.google.ch\" target=\"_blank\">http://www.google.ch</a> text");

		assertEquals(expected.toString(), replacedText);
	}

	@Test
	public void testReplaceUrlAsHtmlLinkWithNames() throws Exception {
		testSameAmountOfAnchorsAndName();
		testLessNames();
		testMoreNames();
	}

	private void testMoreNames() {

		String text = "text	http://www.helio-vo.eu	text http://www.google.ch text";
		String replacedText = StringUtils.replaceUrlAsHtmlAnchor(text, "helio", "google", "bing");

		System.out.println(replacedText);

		StringBuffer expected = new StringBuffer();
		expected.append("text <a href=\"http://www.helio-vo.eu\" target=\"_blank\">helio</a> text ");
		expected.append("<a href=\"http://www.google.ch\" target=\"_blank\">google</a> text");

		assertEquals(expected.toString(), replacedText);
	}

	private void testLessNames() {

		String text = "text	http://www.helio-vo.eu	text http://www.google.ch text";
		String replacedText = StringUtils.replaceUrlAsHtmlAnchor(text, "helio");

		System.out.println(replacedText);

		StringBuffer expected = new StringBuffer();
		expected.append("text <a href=\"http://www.helio-vo.eu\" target=\"_blank\">helio</a> text ");
		expected.append("<a href=\"http://www.google.ch\" target=\"_blank\">http://www.google.ch</a> text");

		assertEquals(expected.toString(), replacedText);
	}

	private void testSameAmountOfAnchorsAndName() {

		String text = "text	http://www.helio-vo.eu	text http://www.google.ch text";
		String replacedText = StringUtils.replaceUrlAsHtmlAnchor(text, "helio", "google");

		System.out.println(replacedText);

		StringBuffer expected = new StringBuffer();
		expected.append("text <a href=\"http://www.helio-vo.eu\" target=\"_blank\">helio</a> text ");
		expected.append("<a href=\"http://www.google.ch\" target=\"_blank\">google</a> text");

		assertEquals(expected.toString(), replacedText);
	}
}