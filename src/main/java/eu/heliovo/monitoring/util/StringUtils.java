package eu.heliovo.monitoring.util;

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
}