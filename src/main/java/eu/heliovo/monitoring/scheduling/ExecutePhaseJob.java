package eu.heliovo.monitoring.scheduling;

import java.util.List;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import eu.heliovo.monitoring.component.AbstractComponent;
import eu.heliovo.monitoring.daemon.RemotingMonitoringDaemon;
import eu.heliovo.monitoring.model.ServiceStatus;

public class ExecutePhaseJob extends QuartzJobBean {

	private final Logger logger = Logger.getLogger(this.getClass());

	private AbstractComponent component;
	private RemotingMonitoringDaemon monitoringDaemon;

	@Override
	protected void executeInternal(final JobExecutionContext context) throws JobExecutionException {

		component.refreshCache();

		final List<ServiceStatus> result = component.getStatus();
		monitoringDaemon.writeServiceStatusToNagios(result);

		if (logger.isDebugEnabled()) {
			logger.debug("refreshed ping cache");
			for (final ServiceStatus serviceStatus : result) {
				logger.debug("service: " + serviceStatus.getId() + " status: " + serviceStatus.getState().toString()
						+ " response time: " + serviceStatus.getResponseTime() + " ms");
			}
		}
	}

	public void setComponent(final AbstractComponent component) {
		this.component = component;
	}

	public void setMonitoringDaemon(final RemotingMonitoringDaemon monitoringDaemon) {
		this.monitoringDaemon = monitoringDaemon;
	}
}