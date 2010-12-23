package eu.heliovo.monitoring.failuredetector;

import static eu.heliovo.monitoring.failuredetector.HostStatisticsFileRecorder.STATISTIC_FILE_SUFFIX;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import eu.heliovo.monitoring.model.*;

public class HostStatisticsFileRecorderTest extends Assert {

	private final File statisticFilesDir = new File(System.getProperty("java.io.tmpdir"), "statistics");

	public HostStatisticsFileRecorderTest() {
		if (!statisticFilesDir.exists()) {
			assertTrue(statisticFilesDir.mkdirs());
		}
	}

	@Test
	public void testRecorder() throws Throwable {

		HostStatisticsFileRecorder recorder = new HostStatisticsFileRecorder(statisticFilesDir.getPath());

		Set<Service> services = Collections.emptySet();
		Host host1 = ModelFactory.newHost(new URL("http://host1.de:1234/"), services);
		Host host2 = ModelFactory.newHost(new URL("http://host2.com:1234/"), services);

		Set<Host> newHosts = new HashSet<Host>();
		newHosts.add(host1);
		newHosts.add(host2);

		recorder.updateHosts(newHosts);

		long entryId = recorder.getNextEntryId(host1);
		long entryId2 = recorder.getNextEntryId(host1);
		assertFalse(entryId == entryId2);

		long entryIdHost2 = recorder.getNextEntryId(host2);

		writeSomeMeasuresForTesting(recorder, host1, host2, entryId, entryId2, entryIdHost2);
		
		recorder.closeAllFileWriters(); // closing all internal fileWriters forces a flush, otherwise the files are empty

		List<String> statisticFiles = Arrays.asList(statisticFilesDir.list());
		assertEquals(2, statisticFiles.size());

		String expectedHost1FileName = host1.getName() + STATISTIC_FILE_SUFFIX;
		String expectedHost2FileName = host2.getName() + STATISTIC_FILE_SUFFIX;

		System.out.println("statisticFilesDir files:");
		for (String string : statisticFiles) {
			System.out.println(string);
		}

		assertTrue(statisticFiles.contains(expectedHost1FileName));
		assertTrue(statisticFiles.contains(expectedHost2FileName));

		File statisticsFileHost1 = new File(statisticFilesDir, expectedHost1FileName);
		File statisticsFileHost2 = new File(statisticFilesDir, expectedHost2FileName);

		List<String> linesHost1 = printContentStatisticsFileHost1(statisticsFileHost1);
		validateContentStatisticsFileHost1(entryId, entryId2, linesHost1);

		List<String> linesHost2 = printContentStatisticsFileHost2(statisticsFileHost2);
		validateContentStatisticsFileHost2(entryIdHost2, linesHost2);

		cleanUp();
	}

	private void validateContentStatisticsFileHost2(long entryIdHost2, List<String> linesHost2) {
		assertTrue(linesHost2.contains(entryIdHost2 + " " + 1225345));
		assertTrue(linesHost2.contains(entryIdHost2 + " " + 23342));
		assertTrue(linesHost2.contains(entryIdHost2 + " " + 87733));
		assertTrue(linesHost2.contains(entryIdHost2 + " " + "java.net.ConnectException: too many connections!"));
	}

	private List<String> printContentStatisticsFileHost2(File statisticsFileHost2) throws IOException {
		System.out.println("content statisticsFileHost2:");
		@SuppressWarnings("unchecked")
		List<String> linesHost2 = FileUtils.readLines(statisticsFileHost2);
		for (String line : linesHost2) {
			System.out.println(line);
		}
		return linesHost2;
	}

	private List<String> printContentStatisticsFileHost1(File statisticsFileHost1) throws IOException {
		System.out.println("content statisticsFileHost1:");
		@SuppressWarnings("unchecked")
		List<String> linesHost1 = FileUtils.readLines(statisticsFileHost1);
		for (String line : linesHost1) {
			System.out.println(line);
		}
		return linesHost1;
	}

	private void validateContentStatisticsFileHost1(long entryId, long entryId2, List<String> linesHost1) {
		assertTrue(linesHost1.contains(entryId + " " + 12345));
		assertTrue(linesHost1.contains(entryId + " " + 232));
		assertTrue(linesHost1.contains(entryId + " " + 122446467));

		assertTrue(linesHost1.contains(entryId2 + " " + 3428));
		assertTrue(linesHost1.contains(entryId2 + " " + 7878878));
		assertTrue(linesHost1.contains(entryId2 + " " + 7878));
	}

	private void writeSomeMeasuresForTesting(HostStatisticsFileRecorder recorder, Host host1, Host host2, long entryId,
			long entryId2, long entryIdHost2) {

		recorder.record(host1, entryId, 12345);
		recorder.record(host1, entryId, 232);
		recorder.record(host1, entryId, 122446467);

		recorder.record(host1, entryId2, 3428);
		recorder.record(host1, entryId2, 7878878);
		recorder.record(host1, entryId2, 7878);

		recorder.record(host2, entryIdHost2, 1225345);
		recorder.record(host2, entryIdHost2, 23342);
		recorder.record(host2, entryIdHost2, 87733);

		recorder.record(host2, entryIdHost2, new ConnectException("too many connections!"));
	}

	private void cleanUp() {
		for (File file : statisticFilesDir.listFiles()) {
			assertTrue(file.delete());
		}
		assertTrue(statisticFilesDir.delete());
	}
}