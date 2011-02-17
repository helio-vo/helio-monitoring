package eu.heliovo.monitoring.util;

import static com.rosaloves.bitlyj.Jmp.as;

import com.rosaloves.bitlyj.*;

/**
 * This Utility shortens URLs with the help of the URL shortener service of bit.ly. It provides an API to do so. Here
 * the libary bitlyj is use to access this API, see http://code.google.com/p/bitlyj/ for more details. An bit.ly account
 * is needed to get an API key to access the bit.ly API, see the site as well. "j.mp" is used instead of "bit.ly" as a
 * shorter representation. It is provides by bit.ly with the same account. The account is registered to the mail address
 * "kevin.seidler@students.fhnw.ch". It can be changed on the bit.ly website using the user and password provided below.
 * 
 * @author Kevin Seidler
 * 
 */
public final class UrlShortener {

	private static final String BITLY_USER = "heliomonitoring"; // password: vUAxchcf
	private static final String BITLY_API_KEY = "R_87f7938d6608fa33caf31e08e4204908";

	private UrlShortener() {
	}

	public static String shorten(String longUrl) {

		Url shortUrl = as(BITLY_USER, BITLY_API_KEY).call(Bitly.shorten(longUrl));

		return shortUrl.getShortUrl();
	}
}