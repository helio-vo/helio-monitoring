package eu.heliovo.monitoring.util;

import static org.springframework.util.StringUtils.arrayToDelimitedString;

import java.net.*;

/**
 * Utility-Methods for manipulating Strings.
 * 
 * @author Kevin Seidler
 * 
 */
public final class StringUtils {

	private StringUtils() {
	}

	/**
	 * Returns the canonical text of a given text to use it in file names or any other cases where whitespaces and
	 * special characters are not allowed.
	 */
	public static String getCanonicalString(String text) {

		String canoncialText = text.replace(' ', '_');
		canoncialText = canoncialText.replace("\\", "");
		canoncialText = canoncialText.replace("/", "");

		return canoncialText;
	}

	/**
	 * Returns a new string replacing all URLs with a HTML anchor and its anchor name in the given order.
	 */
	public static String replaceUrlAsHtmlAnchor(String text, String... anchorNames) {

		// separete input by spaces (URLs don't have spaces)
		String[] textParts = text.split("\\s");

		int nextAnchorNameIndex = 0;

		// attempt to convert each item into an URL.
		for (int i = 0; i < textParts.length; i++) {

			try {
				URL url = new URL(textParts[i]);

				// if possible then replace with anchor

				String anchorName = getAnchorName(nextAnchorNameIndex, url, anchorNames);

				nextAnchorNameIndex++;

				textParts[i] = "<a href=\"" + url + "\" target=\"_blank\">" + anchorName + "</a>";

			} catch (MalformedURLException e) {
				// if it was not a URL
			}
		}

		return arrayToDelimitedString(textParts, " ");
	}

	private static String getAnchorName(int nextAnchorNameIndex, URL url, String... anchorNames) {
		return nextAnchorNameIndex < anchorNames.length ? anchorNames[nextAnchorNameIndex] : url.toString();
	}
}