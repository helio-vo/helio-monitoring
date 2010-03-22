package eu.heliovo.monitoring.scheduling;

import java.util.List;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import eu.heliovo.monitoring.component.PingComponent;
import eu.heliovo.monitoring.daemon.MonitoringDaemon;
import eu.heliovo.monitoring.model.ServiceStatus;

public class PingJob extends QuartzJobBean {

	private final Logger logger = Logger.getLogger(this.getClass());

	private PingComponent pingComponent;
	private MonitoringDaemon monitoringDaemon;

	@Override
	protected void executeInternal(final JobExecutionContext context) throws JobExecutionException {

		pingComponent.refreshCache();

		final List<ServiceStatus> result = pingComponent.getStatus();
		monitoringDaemon.writeServiceStatusToNagios(result);

		if (logger.isDebugEnabled()) {
			logger.debug("refreshed ping cache");
			for (final ServiceStatus serviceStatus : result) {
				logger.debug("service: " + serviceStatus.getId() + " status: " + serviceStatus.getState().toString()
						+ " response time: " + serviceStatus.getResponseTime() + " ms");
			}
		}
	}

	public void setPingComponent(final PingComponent pingComponent) {
		this.pingComponent = pingComponent;
	}

	public void setMonitoringDaemon(final MonitoringDaemon monitoringDaemon) {
		this.monitoringDaemon = monitoringDaemon;
	}
}