package eu.heliovo.monitoring.util;

import java.util.*;

/**
 * Utility methods in the context of reflection.
 * 
 * @author Kevin Seidler
 * 
 */
public final class ReflectionUtils {

	private ReflectionUtils() {
	}

	public static boolean implementsInterface(Object object, Class<?> interfaze) {
		List<Class<?>> interfaces = Arrays.asList(object.getClass().getInterfaces());
		return interfaces.contains(interfaze);
	}

}
