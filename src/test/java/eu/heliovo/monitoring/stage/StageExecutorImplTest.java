package eu.heliovo.monitoring.stage;

import static eu.heliovo.monitoring.model.ModelFactory.newHost;
import static eu.heliovo.monitoring.model.ModelFactory.newService;
import static eu.heliovo.monitoring.model.ModelFactory.newStatusDetails;
import static eu.heliovo.monitoring.model.Status.CRITICAL;
import static eu.heliovo.monitoring.model.Status.OK;
import static java.util.Arrays.asList;

import java.net.URL;
import java.util.*;

import org.junit.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import eu.heliovo.monitoring.exporter.StatusDetailsExporter;
import eu.heliovo.monitoring.model.*;

public class StageExecutorImplTest extends Assert {

	private boolean pingStageCalled = false;
	private boolean methodCallStageCalled = false;
	private boolean testingStageCalled = false;
	private boolean hostStatusDetailsExported = false;
	private boolean serviceStatusDetailsExported = false;

	@Test
	public void testExecutor() throws Exception {

		StageExecutor executor = initStageExecutor();
		executor.execute();

		verify(executor.getStatus());
	}

	@Test
	public void testScheduledExecution() throws Exception {

		StageExecutorImpl executor = initStageExecutor();

		executor.doContinousExecution();

		verifyResult(executor);
	}

	@After
	public void cleanUp() {
		pingStageCalled = false;
		methodCallStageCalled = false;
		testingStageCalled = false;
		hostStatusDetailsExported = false;
		serviceStatusDetailsExported = false;
	}

	private void verifyResult(StageExecutorImpl executor) throws InterruptedException {

		List<StatusDetails<Service>> latestStatus = executor.getStatus();
		assertEquals(0, latestStatus.size());

		// START_DELAY + 100 ms Threas.sleep in Ping Stage, so no result should be calculated
		// Thread.sleep(510);

		latestStatus = executor.getStatus();
		assertEquals(0, latestStatus.size());

		Thread.sleep(1000);

		latestStatus = executor.getStatus();
		assertEquals(4, latestStatus.size());
	}

	private ThreadPoolTaskScheduler initScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		return taskScheduler;
	}

	private void verify(List<StatusDetails<Service>> result) {

		assertTrue(pingStageCalled);
		assertTrue(methodCallStageCalled);
		assertTrue(testingStageCalled);
		assertTrue(hostStatusDetailsExported);
		assertTrue(serviceStatusDetailsExported);

		assertEquals(4, result.size());

		boolean icsFound = false;
		boolean ilsFound = false;
		boolean hfcFound = false;
		boolean dpasFound = false;

		System.out.println("result:");
		for (StatusDetails<Service> statusDetails : result) {

			System.out.println(statusDetails);

			if (statusDetails.getName().equals("ICS")) {
				icsFound = true;
				assertEquals(CRITICAL, statusDetails.getStatus());
			}
			
			if (statusDetails.getName().equals("ILS")) {
			    ilsFound = true;
			    assertEquals(OK, statusDetails.getStatus());
			}

			if (statusDetails.getName().equals("HFC")) {
				hfcFound = true;
				assertEquals(CRITICAL, statusDetails.getStatus());
			}

			if (statusDetails.getName().equals("DPAS")) {
				dpasFound = true;
				assertEquals(CRITICAL, statusDetails.getStatus());
			}
		}

		assertTrue(icsFound);
		assertTrue(ilsFound);
		assertTrue(hfcFound);
		assertTrue(dpasFound);
	}

	/**
	 * Create a stage executor with four registered services and assign a handler for every stage.
	 * @return the executor impl.
	 * @throws Exception
	 */
	private StageExecutorImpl initStageExecutor() throws Exception {
		PingStage pingstage = initPingStage();
		MethodCallStage methodCallStage = initMethodCallStage();
		TestingStage testingStage = initTestingStage();
		StatusDetailsExporter statusDetailsExporter = initStatusDetailsExporter();
		ThreadPoolTaskScheduler taskScheduler = initScheduler();
		int executionDelayInMillis = 100;

		StageExecutorImpl executor;
		executor = new StageExecutorImpl(pingstage, methodCallStage, testingStage, statusDetailsExporter,
				taskScheduler, executionDelayInMillis);

		Set<Host> hosts = new HashSet<Host>();

		URL icsUrl = new URL("http://msslkz.mssl.ucl.ac.uk/helio-ics/HelioService?wsdl");
		URL dpasUrl = new URL("http://msslkz.mssl.ucl.ac.uk/helio-dpas/HelioService?wsdl");
		URL ilsUrl = new URL("http://msslkz.mssl.ucl.ac.uk/helio-ils/HelioService?wsdl");

		Set<Service> services = new HashSet<Service>();
		services.add(newService("", "ICS", icsUrl));
		services.add(newService("", "DPAS", dpasUrl));
		services.add(newService("", "ILS", ilsUrl));

		hosts.add(newHost(icsUrl, services));

		URL hfcUrl = new URL("http://voparis-helio.obspm.fr/helio-hfc");
		services = new HashSet<Service>(asList(newService("", "HFC", hfcUrl)));
		hosts.add(newHost(hfcUrl, services));

		executor.updateHosts(hosts);

		return executor;
	}

	private StatusDetailsExporter initStatusDetailsExporter() {
		StatusDetailsExporter statusDetailsExporter = new StatusDetailsExporter() {
			@Override
			public void exportHostStatusDetails(List<StatusDetails<Host>> hostStatusDetails) {

				hostStatusDetailsExported = true;
				System.out.println("exported hosts: ");
				for (StatusDetails<?> statusDetails : hostStatusDetails) {
					System.out.println(statusDetails);
				}
				assertEquals(2, hostStatusDetails.size());
			}

			@Override
			public void exportServiceStatusDetails(List<StatusDetails<Service>> serviceStatusDetails) {

				serviceStatusDetailsExported = true;
				System.out.println("exported services: ");
				for (StatusDetails<?> statusDetails : serviceStatusDetails) {
					System.out.println(statusDetails);
				}
				assertEquals(4, serviceStatusDetails.size());
			}
		};
		return statusDetailsExporter;
	}

	private PingStage initPingStage() {

		PingStage pingstage = new PingStage() {
			@Override
			public List<StatusDetails<Host>> getStatus(Set<Host> hosts) {

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}

				pingStageCalled = true;

				List<StatusDetails<Host>> hostStatusDetails = new ArrayList<StatusDetails<Host>>();
				for (Host host : hosts) {
					Status status = OK;
					long responseTime = Long.MAX_VALUE;
					String hostName = host.getName();
					String message = "";

					if ("msslkz.mssl.ucl.ac.uk".equals(host.getName())) {
						responseTime = 54;
						message = "Service is " + host + " reachable";
					}
					if ("voparis-helio.obspm.fr".equals(host.getName())) {
						status = CRITICAL;
						message = "Service host " + host + " is unreachable";
					}

					hostStatusDetails
							.add(newStatusDetails(host, hostName, host.getUrl(), status, responseTime, message));
				}

				return hostStatusDetails;
			}
		};
		return pingstage;
	}

	private MethodCallStage initMethodCallStage() {
		MethodCallStage methodCallStage = new MethodCallStage() {
			@Override
			public List<StatusDetails<Service>> getStatus(Set<Service> services) {

				methodCallStageCalled = true;
				// one service failed in ping status. thus there are just three left.
				assertEquals(3, services.size());

				List<StatusDetails<Service>> statusDetails = new ArrayList<StatusDetails<Service>>();
				for (Service service : services) {
					if ("ICS".equals(service.getName())) {
						String message = "Service is working";
						statusDetails.add(newStatusDetails(service, service.getName(), service.getUrl(), OK, 83,
								message));
					}

					if ("DPAS".equals(service.getName())) {
						String message = "Service is not working";
						statusDetails.add(newStatusDetails(service, service.getName(), service.getUrl(), CRITICAL,
								Long.MAX_VALUE, message));
					}

					if ("ILS".equals(service.getName())) {
						String message = "Service is working";
						statusDetails.add(newStatusDetails(service, service.getName(), service.getUrl(), OK, 78,
								message));
					}					
				}

				assertEquals(3, statusDetails.size());

				return statusDetails;
			}
		};
		return methodCallStage;
	}

	private TestingStage initTestingStage() {
		TestingStage testingStage = new TestingStage() {

			@Override
			public List<StatusDetails<Service>> getStatus(Set<Service> services) {

				testingStageCalled = true;
				assertEquals(2, services.size());

				List<StatusDetails<Service>> statusDetails = new ArrayList<StatusDetails<Service>>();
				for (Service service : services) {

					if ("ICS".equals(service.getName())) {
						String serviceName = service.getName();
						String message = "0/2 Tests successful";
						statusDetails
								.add(newStatusDetails(service, serviceName, service.getUrl(), CRITICAL, 0, message));
					}

					if ("ILS".equals(service.getName())) {
						String serviceName = service.getName();
						String message = "2/2 Tests successful";
						statusDetails.add(newStatusDetails(service, serviceName, service.getUrl(), OK, 78, message));
					}

				}

				assertEquals(2, statusDetails.size());

				return statusDetails;
			}
		};
		return testingStage;
	}
}