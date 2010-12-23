package eu.heliovo.monitoring.service;

import static eu.heliovo.monitoring.test.util.TestUtils.getStageHelper;
import static eu.heliovo.monitoring.test.util.TestUtils.logFilesUrl;

import java.net.*;
import java.util.*;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.exporter.ServiceStatusDetailsExporter;
import eu.heliovo.monitoring.failuredetector.*;
import eu.heliovo.monitoring.listener.ServiceUpdateListener;
import eu.heliovo.monitoring.logging.LoggingTestUtils;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.serviceloader.*;
import eu.heliovo.monitoring.serviceloader.ServiceLoader;
import eu.heliovo.monitoring.stage.*;
import eu.heliovo.monitoring.test.util.TestServices;

public class MonitoringServiceTest extends Assert {

	private Set<Service> testServices = new HashSet<Service>();
	private int timesUpdateReceived = 0;

	@Test
	public void testService() throws Exception {

		MonitoringServiceImpl monitoringService = initMonitoringService();

		monitoringService.updateServices();

		// these lines are automatically called by spring
		monitoringService.updatePingStatusAndExport();
		monitoringService.updateMethodCallStatusAndExport();
		monitoringService.updateTestingStatusAndExport();

		List<ServiceStatusDetails> result = monitoringService.getPingStatus();
		validateStageResult(result);

		result = monitoringService.getMethodCallStatus();
		validateStageResult(result);

		result = monitoringService.getTestingStatus();
		validateStageResult(result);
	}

	private MonitoringServiceImpl initMonitoringService() throws Exception {

		StageHelper stageHelper = getStageHelper();
		ServiceFailureDetector failureDetector = FailureDetectorTestUtils.getServiceFailureDetector();

		PingStage pingStage = new PingStage(failureDetector);
		MethodCallStage methodCallStage = new MethodCallStage(stageHelper, LoggingTestUtils.getLoggingFactory(),
				logFilesUrl);
		TestingStage testingStage = new TestingStage(stageHelper, LoggingTestUtils.getLoggingFactory(), logFilesUrl);

		ServiceLoader serviceLoader = new StaticServiceLoader();

		ServiceStatusDetailsExporter exporter = new ServiceStatusDetailsExporter() {
			@Override
			public void exportServiceStatusDetails(List<ServiceStatusDetails> serviceStatus) {
			}
		};

		List<ServiceUpdateListener> listener = Arrays.asList(new ServiceUpdateListener[] { failureDetector,
				methodCallStage, testingStage });

		MonitoringServiceImpl monitoringService = new MonitoringServiceImpl(pingStage, methodCallStage, testingStage,
				serviceLoader, exporter, listener);

		return monitoringService;
	}

	private void validateStageResult(List<ServiceStatusDetails> result) {
		assertFalse(result.isEmpty());
		for (ServiceStatusDetails serviceStatusDetails : result) {
			System.out.println(serviceStatusDetails);
		}
	}

	@Test
	public void testUpdateServices() throws Exception {

		MonitoringServiceImpl monitoringService = initMonitoringServiceForUpdateServices();

		testNoChangeInList(monitoringService);
		testRemoveService(monitoringService);
		Service someService = testAddNewService(monitoringService);
		testRemoveAndAddNewServiceWithSameData(monitoringService, someService);
		testPositionChange(monitoringService);
		initExampleList(monitoringService);
		testNewListWithSameServices(monitoringService);
	}

	private MonitoringServiceImpl initMonitoringServiceForUpdateServices() {
		ServiceLoader testServiceLoader = createTestServiceLoader();
		ServiceUpdateListener testListener = createTestServiceUpdateListener();
		List<ServiceUpdateListener> listener = Arrays.asList(new ServiceUpdateListener[] { testListener });
		MonitoringServiceImpl monitoringService = new MonitoringServiceImpl(null, null, null, testServiceLoader, null,
				listener);
		return monitoringService;
	}

	private ServiceLoader createTestServiceLoader() {
		testServices.addAll(TestServices.LIST); // because TestServices.LIST is unmodifiable, but we need to modify it
		ServiceLoader serviceLoader = new ServiceLoader() {
			@Override
			public Set<Service> loadServices() {
				return testServices;
			}
		};
		return serviceLoader;
	}

	private ServiceUpdateListener createTestServiceUpdateListener() {

		ServiceUpdateListener testListener = new ServiceUpdateListener() {
			@Override
			public void updateServices(Set<Service> newServices) {

				timesUpdateReceived++;

				System.out.println("services update Nr. " + timesUpdateReceived + " received: ");
				for (Service service : newServices) {
					System.out.println(service.getName() + " " + service.getUrl());
				}
				System.out.println();
			}
		};
		return testListener;
	}

	private void testNoChangeInList(MonitoringServiceImpl monitoringService) {
		monitoringService.updateServices();
		monitoringService.updateServices();
		assertEquals(1, timesUpdateReceived);
	}

	private void testRemoveService(MonitoringServiceImpl monitoringService) {
		testServices.remove(getAnyTestService());
		monitoringService.updateServices();
		assertEquals(2, timesUpdateReceived);
		monitoringService.updateServices();
		assertEquals(2, timesUpdateReceived);
	}

	private Service getAnyTestService() {
		return testServices.iterator().next();
	}

	private Service testAddNewService(MonitoringServiceImpl monitoringService) throws MalformedURLException {
		Service someService = ModelFactory.newService("SomeService", new URL("http://www.helio-vo.eu/"));
		testServices.add(someService);
		monitoringService.updateServices();
		assertEquals(3, timesUpdateReceived);
		monitoringService.updateServices();
		assertEquals(3, timesUpdateReceived);
		return someService;
	}

	private void testRemoveAndAddNewServiceWithSameData(MonitoringServiceImpl monitoringService, Service someService)
			throws Exception {

		testServices.remove(someService);
		testServices.add(ModelFactory.newService("SomeService", new URL("http://www.helio-vo.eu/")));
		monitoringService.updateServices();
		assertEquals(3, timesUpdateReceived);
	}

	private void testPositionChange(MonitoringServiceImpl monitoringService) {
		Service service = getAnyTestService();
		testServices.remove(service);
		testServices.add(service);
		monitoringService.updateServices();
		assertEquals(3, timesUpdateReceived);
	}

	private void initExampleList(MonitoringServiceImpl monitoringService) throws MalformedURLException {
		testServices.clear();
		testServices.add(ModelFactory.newService("SomeService", new URL("http://www.helio-vo.eu/")));
		testServices.addAll(TestServices.LIST);
		monitoringService.updateServices();
		assertEquals(4, timesUpdateReceived);
	}

	private void testNewListWithSameServices(MonitoringServiceImpl monitoringService) throws Exception {
		testServices = new HashSet<Service>();
		testServices.addAll(TestServices.LIST);
		testServices.add(ModelFactory.newService("SomeService", new URL("http://www.helio-vo.eu/")));
		monitoringService.updateServices();
		assertEquals(4, timesUpdateReceived);
	}
}