package eu.heliovo.monitoring.util;

import java.util.Arrays;
import java.util.List;

public final class ReflectionUtils {

	private ReflectionUtils() {
	}

	public static boolean implementsInterface(final Object object, final Class<?> interfaze) {
		final List<Class<?>> interfaces = Arrays.asList(object.getClass().getInterfaces());
		return interfaces.contains(interfaze);
	}

}
