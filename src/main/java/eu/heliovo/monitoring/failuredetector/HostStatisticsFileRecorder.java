package eu.heliovo.monitoring.failuredetector;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;

import eu.heliovo.monitoring.model.Host;
import eu.heliovo.monitoring.validator.DirectoryAccessValidator;

/**
 * Just to record the measures of the failure detector for a later evaluation.<br/>
 * TODO write parser and show measures in diagrams
 * 
 * @author Kevin Seidler
 * 
 */
@Component
public final class HostStatisticsFileRecorder implements HostStatisticsRecorder {

	protected static final String STATISTIC_FILE_SUFFIX = ".dat";

	private final File statisticFilesDir;

	private final Logger logger = Logger.getLogger(this.getClass());

	private final Map<Host, EntryIdFileWriter> fileWriters = new ConcurrentHashMap<Host, EntryIdFileWriter>();

	@Autowired
	protected HostStatisticsFileRecorder(
			@Value("${failureDetector.statisticsRecorder.statisticFilesDir}") String statisticFilesDir) {

		this.statisticFilesDir = initStatisticFilesDir(statisticFilesDir);
		DirectoryAccessValidator.validate(this.statisticFilesDir);
	}

	private File initStatisticFilesDir(String statisticFilesDirPath) {
		File statisticFilesDir = new File(statisticFilesDirPath);
		try {
			statisticFilesDir.mkdirs();
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		return statisticFilesDir;
	}
	
	@Override
	public void updateHosts(List<Host> newHosts) {
		removeOldFileWriters(newHosts);
		addNewFileWriters(newHosts);
	}
	
	private void removeOldFileWriters(List<Host> newHosts) {
		for (Host hostFromStatistics : fileWriters.keySet()) {
			if (!newHosts.contains(hostFromStatistics)) {
				fileWriters.remove(hostFromStatistics);
			}
		}
	}
	
	private void addNewFileWriters(List<Host> newHosts) {
		for (Host newHost : newHosts) {
			if (!fileWriters.containsKey(newHost)) {
				try {
					File statisticsFile = new File(statisticFilesDir, newHost.getName() + STATISTIC_FILE_SUFFIX);
					fileWriters.put(newHost, new EntryIdFileWriter(statisticsFile));
				} catch (IOException e) {
					logger.debug(e.getMessage(), e);
				}
			}
		}
	}
	
	protected void closeAllFileWriters() {
		for (EntryIdFileWriter fileWriter : fileWriters.values()) {
			try {
				fileWriter.close();
			} catch (IOException e) {
				logger.debug(e.getMessage(), e);
			}
		}
	}

	@Override
	public void record(Host host, long entryId, long measure) {
		try {
			String text = entryId + " " + measure + "\n";
			EntryIdFileWriter fileWriter = fileWriters.get(host);
			fileWriter.write(text);
		} catch (IOException e) {
			logger.debug(e.getMessage(), e);
		}
	}

	@Override
	public long getNextEntryId(Host host) {
		EntryIdFileWriter fileWriter = fileWriters.get(host);
		return fileWriter.getNextEntryId();
	}

	@Override
	protected void finalize() throws Throwable {
		closeAllFileWriters();
	}

	private static final class EntryIdFileWriter {

		private long entryId;
		private final FileWriter fileWriter;

		protected EntryIdFileWriter(File file) throws IOException {
			this.fileWriter = new FileWriter(file, true);
		}

		protected long getNextEntryId() {
			return entryId++;
		}

		protected void write(String text) throws IOException {
			fileWriter.write(text);
		}

		protected void close() throws IOException {
			fileWriter.close();
		}
	}
}