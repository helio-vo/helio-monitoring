package eu.heliovo.monitoring.validator;

import java.io.*;

import junit.framework.Assert;

import org.junit.Test;

public class DirectoryAccessValidatorTest extends Assert {

	private final String javaTmpDir = System.getProperty("java.io.tmpdir");

	@Test
	public void testValidation() throws Exception {

		File testDirectory = new File(javaTmpDir, "testDirectory");

		testNotExisting(testDirectory);

		assertTrue(testDirectory.mkdir());
		DirectoryAccessValidator.validate(testDirectory); // should not throw an exception yet

		testNotDirectory();

		testNotExecutable(testDirectory);
		testNotReadable(testDirectory);
		testNotWritable(testDirectory);

		cleanUp(testDirectory);
	}

	private void cleanUp(File testDirectory) {
		testDirectory.setExecutable(true);
		testDirectory.setReadable(true);
		testDirectory.setWritable(true);
		assertTrue(testDirectory.delete());
	}

	private void testNotWritable(File testDirectory) {
		testDirectory.setWritable(false);
		boolean exceptionThrown = false;
		try {
			DirectoryAccessValidator.validate(testDirectory);
		} catch (Exception e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}

	private void testNotReadable(File testDirectory) {
		testDirectory.setReadable(false);
		boolean exceptionThrown = false;
		try {
			DirectoryAccessValidator.validate(testDirectory);
		} catch (Exception e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}

	private void testNotExecutable(File testDirectory) {
		testDirectory.setExecutable(false);
		boolean exceptionThrown = false;
		try {
			DirectoryAccessValidator.validate(testDirectory);
		} catch (Exception e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}

	private void testNotExisting(File testDirectory) {
		boolean exceptionThrown = false;
		try {
			DirectoryAccessValidator.validate(testDirectory);
		} catch (Exception e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}

	private void testNotDirectory() throws IOException {
		File testFakeDir = new File(javaTmpDir, "testFakeDir");
		assertTrue(testFakeDir.createNewFile());
		boolean exceptionThrown = false;
		try {
			DirectoryAccessValidator.validate(testFakeDir);
		} catch (Exception e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		assertTrue(testFakeDir.delete());
	}
}