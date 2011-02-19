package eu.heliovo.monitoring.service;

import static java.util.Collections.emptyList;

import java.net.*;
import java.util.*;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.listener.ServiceUpdateListener;
import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.serviceloader.*;
import eu.heliovo.monitoring.serviceloader.ServiceLoader;
import eu.heliovo.monitoring.stage.StageExecutor;
import eu.heliovo.monitoring.test.util.TestServices;

public class MonitoringServiceImplTest extends Assert {

	private Set<Service> testServices = new HashSet<Service>();
	private int timesUpdateReceived = 0;

	public MonitoringServiceImplTest() throws Exception {
	}

	@Test
	public void testService() throws Exception {

		MonitoringServiceImpl monitoringService = initMonitoringService();

		monitoringService.updateServices();
		testGetAllStatus(monitoringService);
		testGetStatus(monitoringService);
	}

	private void testGetStatus(MonitoringServiceImpl monitoringService) {

		testNullIdentifier(monitoringService);
		testEmptyIdentifier(monitoringService);
		testNotExistingIdentifier(monitoringService);
		testExistingIdentifier(monitoringService);
	}

	private void testExistingIdentifier(MonitoringServiceImpl monitoringService) {

		StatusDetails<Service> singleResult = monitoringService.getStatus("ICS");
		assertNotNull(singleResult);

		assertEquals("ICS", singleResult.getName());
		assertEquals(Status.OK, singleResult.getStatus());

		assertTrue(singleResult.getUrl() == null);
		assertEquals(67, singleResult.getResponseTimeInMillis());
		assertEquals("", singleResult.getMessage());
	}

	private void testNullIdentifier(MonitoringServiceImpl monitoringService) {

		boolean exceptionCaught = false;
		try {
			monitoringService.getStatus(null);
		} catch (IllegalArgumentException e) {
			exceptionCaught = true;
		}

		assertTrue(exceptionCaught);
	}

	private void testEmptyIdentifier(MonitoringServiceImpl monitoringService) {

		boolean exceptionCaught;
		exceptionCaught = false;
		try {
			monitoringService.getStatus("");
		} catch (IllegalArgumentException e) {
			exceptionCaught = true;
		}

		assertTrue(exceptionCaught);
	}

	private void testNotExistingIdentifier(MonitoringServiceImpl monitoringService) {

		boolean exceptionCaught;
		exceptionCaught = false;
		try {
			monitoringService.getStatus("notExistingIdentifier");
		} catch (IllegalArgumentException e) {
			exceptionCaught = true;
		}

		assertTrue(exceptionCaught);
	}

	private void testGetAllStatus(MonitoringServiceImpl monitoringService) {
		List<StatusDetails<Service>> result = monitoringService.getAllStatus();
		assertNotNull(result);
		assertEquals(1, result.size());

		boolean icsFound = true;
		for (StatusDetails<Service> statusDetails : result) {
			if (statusDetails.getName().equals("ICS")) {
				assertEquals(Status.OK, statusDetails.getStatus());
				assertEquals(67, statusDetails.getResponseTimeInMillis());
				assertEquals("ICS", statusDetails.getMonitoredEntity().getName());
			}
		}
		assertTrue(icsFound);
	}

	private MonitoringServiceImpl initMonitoringService() throws Exception {

		ServiceLoader serviceLoader = new StaticServiceLoader();
		StageExecutor stageExecutor = new StageExecutor() {

			@Override
			public void updateHosts(Set<Host> newHosts) {
			}

			@Override
			public List<StatusDetails<Service>> getStatus() {

				Service ics = ModelFactory.newService("ICS", "ICS", null);
				StatusDetails<Service> icsStatus = ModelFactory.newStatusDetails(ics, "ICS", null, Status.OK, 67, "");

				List<StatusDetails<Service>> status = new ArrayList<StatusDetails<Service>>();
				status.add(icsStatus);

				return status;
			}

			@Override
			public void execute() {
			}

			@Override
			public void doContinousExecution() {
			}
		};

		List<ServiceUpdateListener> servUpdateListeners = emptyList();
		return new MonitoringServiceImpl(serviceLoader, stageExecutor, servUpdateListeners);
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
		testEmptyList(monitoringService);
	}

	private MonitoringServiceImpl initMonitoringServiceForUpdateServices() {

		ServiceLoader testServiceLoader = createTestServiceLoader();
		ServiceUpdateListener testListener = createTestServiceUpdateListener();
		List<ServiceUpdateListener> listener = Arrays.asList(testListener);

		return new MonitoringServiceImpl(testServiceLoader, null, listener);
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
		Service someService = ModelFactory.newService("", "SomeService", new URL("http://www.helio-vo.eu/"));
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
		testServices.add(ModelFactory.newService("", "SomeService", new URL("http://www.helio-vo.eu/")));
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
		testServices.add(ModelFactory.newService("", "SomeService", new URL("http://www.helio-vo.eu/")));
		testServices.addAll(TestServices.LIST);
		monitoringService.updateServices();
		assertEquals(4, timesUpdateReceived);
	}

	private void testNewListWithSameServices(MonitoringServiceImpl monitoringService) throws Exception {
		testServices = new HashSet<Service>();
		testServices.addAll(TestServices.LIST);
		testServices.add(ModelFactory.newService("", "SomeService", new URL("http://www.helio-vo.eu/")));
		monitoringService.updateServices();
		assertEquals(4, timesUpdateReceived);
	}

	private void testEmptyList(MonitoringServiceImpl monitoringService) {

		// receiving an empty services list is probably an error, caused by communication issues with the registry
		testServices = new HashSet<Service>();
		testServices.addAll(TestServices.LIST);

		monitoringService.updateServices();
		assertEquals(5, timesUpdateReceived);

		testServices = new HashSet<Service>();
		monitoringService.updateServices();
		assertEquals(5, timesUpdateReceived);
	}
}