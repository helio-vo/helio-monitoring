package eu.heliovo.monitoring.validator;

import java.io.File;

/**
 * Validates file requirements which must be given by a directory to access it.
 * 
 * @author Kevin Seidler
 * 
 */
public final class DirectoryAccessValidator {

	private DirectoryAccessValidator() {
	}

	public static void validate(File directory) {
		validateExisting(directory);
		validateDirectory(directory);
		validateAccessRights(directory);
	}

	private static void validateAccessRights(File directory) {
		if (!hasNeededAccess(directory)) {
			throw new IllegalArgumentException("directory " + directory.getAbsolutePath() + " does not have needed access rights [r=" + directory.canRead() + ",w=" + directory.canWrite() + ",x=" + directory.canExecute()+"]");
		}
	}

	private static void validateDirectory(File directory) {
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("this is not a directory!");
		}
	}

	private static void validateExisting(File directory) {
		if (!isExisting(directory)) {
			throw new IllegalArgumentException("Directory '" + directory.getAbsolutePath() + "' does not exist!");
		}
	}

	private static boolean isExisting(File directory) {
		return directory != null && directory.exists();
	}

	private static boolean hasNeededAccess(File directory) {
		return directory.canRead() && directory.canWrite() && directory.canExecute();
	}
}