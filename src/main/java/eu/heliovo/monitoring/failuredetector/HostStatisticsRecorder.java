package eu.heliovo.monitoring.failuredetector;

import eu.heliovo.monitoring.listener.HostUpdateListener;
import eu.heliovo.monitoring.model.Host;

/**
 * The {@link HostStatisticsRecorder} records statistical measures for later examination.
 * 
 * @author Kevin Seidler
 * 
 */
public interface HostStatisticsRecorder extends HostUpdateListener {

	/**
	 * Returns the next entry id for the given host which is stored and determined by the {@link HostStatisticsRecorder}
	 * .
	 */
	long getNextEntryId(Host host);

	/**
	 * Records a measure for a certain host given the entryId.
	 * 
	 * @param host the host to record a measure for
	 * @param entryId many measures can be grouped to one entry
	 * @param measure the measured value
	 */
	void record(Host host, long entryId, long measure);

	/**
	 * Records an error occured while measuring.
	 * 
	 * @param host the host to record an error for
	 * @param entryId many measures/errors can be grouped to one entry
	 * @param error the occored exception
	 */
	void record(Host host, long entryId, Exception error);
}