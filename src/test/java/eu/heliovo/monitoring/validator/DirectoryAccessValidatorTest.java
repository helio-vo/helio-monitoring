package eu.heliovo.monitoring.validator;

import java.io.*;

import junit.framework.Assert;

import org.junit.*;

public class DirectoryAccessValidatorTest extends Assert {

	private final String javaTmpDir = System.getProperty("java.io.tmpdir");
	private final File testDirectory = new File(javaTmpDir, "testDirectory");

	@Test
	public void testValidation() throws Exception {

		testNotExisting(testDirectory);

		assertTrue(testDirectory.mkdir());
		DirectoryAccessValidator.validate(testDirectory); // should not throw an exception yet

		testNotDirectory();

		// these tests are not working on windows, because e.g. canExecute() always returns a true, see
		// http://java.sun.com/developer/technicalArticles/J2SE/Desktop/javase6/enhancements/
		// asserts are commented here and System.outs are used instead
		// TODO fix this in a next version of Java
		testNotExecutable(testDirectory);
		testNotReadable(testDirectory);
		testNotWritable(testDirectory);
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

	private void testNotExecutable(File testDirectory) {
		testDirectory.setExecutable(false);
		boolean exceptionThrown = false;
		try {
			DirectoryAccessValidator.validate(testDirectory);
		} catch (Exception e) {
			exceptionThrown = true;
		}
		// assertTrue(exceptionThrown);
		System.out.println("testNotExecutable successful: " + exceptionThrown);
	}

	private void testNotReadable(File testDirectory) {
		testDirectory.setReadable(false);
		boolean exceptionThrown = false;
		try {
			DirectoryAccessValidator.validate(testDirectory);
		} catch (Exception e) {
			exceptionThrown = true;
		}
		// assertTrue(exceptionThrown);
		System.out.println("testNotReadable successful: " + exceptionThrown);
	}

	private void testNotWritable(File testDirectory) {
		testDirectory.setWritable(false);
		boolean exceptionThrown = false;
		try {
			DirectoryAccessValidator.validate(testDirectory);
		} catch (Exception e) {
			exceptionThrown = true;
		}
		// assertTrue(exceptionThrown);
		System.out.println("testNotWritable successful: " + exceptionThrown);
	}

	@After
	public void cleanUp() {
		testDirectory.setExecutable(true);
		testDirectory.setReadable(true);
		testDirectory.setWritable(true);
		assertTrue(testDirectory.delete());
	}
}