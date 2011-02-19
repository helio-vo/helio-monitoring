package eu.heliovo.monitoring.util;

import java.net.*;

import junit.framework.Assert;

import org.junit.Test;

public class UrlShortenerTest extends Assert {

	@Test
	public void testUrlShortener() {

		String longUrl;
		longUrl = "http://helio-dev.i4ds.ch:8080/helio-monitoring/logs/helio_uoc_trieste_method-call_-1297093260405.txt";
		String shortUrl = UrlShortener.shorten(longUrl);

		System.out.println("longUrl: " + longUrl);
		System.out.println("shortUrl: " + shortUrl);

		assertTrue(shortUrl.length() <= longUrl.length());

		testWellformedUrl(shortUrl);

		// 2nd test for difference
		longUrl = "http://helio-dev.i4ds.ch:8080/helio-monitoring/logs/helio_hec_trieste_method-call_-1297109462190.txt";
		String shortUrl2 = UrlShortener.shorten(longUrl);

		System.out.println("longUrl: " + longUrl);
		System.out.println("shortUrl: " + shortUrl2);

		assertTrue(shortUrl.length() <= longUrl.length());

		testWellformedUrl(shortUrl2);

		assertFalse(shortUrl.equals(shortUrl2));
	}

	private void testWellformedUrl(String shortUrl) {

		boolean urlWellformed = true;
		try {
			new URL(shortUrl);
		} catch (MalformedURLException e) {
			urlWellformed = false;
		}
		assertTrue(urlWellformed);
	}
}