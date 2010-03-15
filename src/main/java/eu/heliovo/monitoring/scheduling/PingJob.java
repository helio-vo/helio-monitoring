package eu.heliovo.monitoring.scheduling;

import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import eu.heliovo.monitoring.component.PingComponent;
import eu.heliovo.monitoring.model.ServiceStatus;

public class PingJob extends QuartzJobBean {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private PingComponent pingComponent;

	@Override
	protected void executeInternal(final JobExecutionContext context) throws JobExecutionException {

		pingComponent.refreshCache();

		if (logger.isDebugEnabled()) {
			logger.debug("refreshed ping cache");
			final List<ServiceStatus> result = pingComponent.getStatus();
			for (final ServiceStatus serviceStatus : result) {
				logger.debug("service: " + serviceStatus.getId() + " status: " + serviceStatus.getState().toString()
						+ " response time: " + serviceStatus.getResponseTime() + " ms");
			}
		}
	}

	public void setPingComponent(final PingComponent pingComponent) {
		this.pingComponent = pingComponent;
	}
}