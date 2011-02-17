package eu.heliovo.monitoring.stage;

import static eu.heliovo.monitoring.logging.LoggingTestUtils.getLoggingFactory;
import static eu.heliovo.monitoring.test.util.TestUtils.getStageHelper;
import static eu.heliovo.monitoring.test.util.TestUtils.logFilesUrl;

import java.util.List;
import java.util.concurrent.ExecutorService;

import junit.framework.Assert;

import org.junit.Test;

import eu.heliovo.monitoring.model.*;
import eu.heliovo.monitoring.test.util.*;

public final class TestingStageImplTest extends Assert {

	private final ExecutorService executor = TestUtils.getExecutor();

	public TestingStageImplTest() throws Exception {
	}

	@Test
	public void testTestStage() throws Exception {

		TestingStage stage = new TestingStageImpl(getStageHelper(), getLoggingFactory(), logFilesUrl, executor);

		List<StatusDetails<Service>> serviceStatus = stage.getStatus(TestServices.LIST);

		assertNotNull(serviceStatus);
		assertTrue(serviceStatus.size() == TestServices.LIST.size());

		boolean testedFakeService = false;
		boolean testedNoWsdlService = false;

		String fakeOfflineServiceName = "FakeOfflineService";
		String noWsdlOfflineServiceName = "NoWsdlOfflineService";

		for (StatusDetails<Service> actualServiceStatusDetails : serviceStatus) {

			Service service = actualServiceStatusDetails.getMonitoredEntity();
			assertTrue(TestServices.LIST.contains(service));

			String originalServiceName = service.getName();
			assertEquals(originalServiceName, actualServiceStatusDetails.getName());

			if (actualServiceStatusDetails.getName().equals(fakeOfflineServiceName)) {
				testedFakeService = true;
				assertTrue(actualServiceStatusDetails.getStatus().equals(Status.CRITICAL));
			}
			if (actualServiceStatusDetails.getName().equals(noWsdlOfflineServiceName)) {
				testedNoWsdlService = true;
				assertTrue(actualServiceStatusDetails.getStatus().equals(Status.CRITICAL));
			}
		}
		assertTrue(testedFakeService);
		assertTrue(testedNoWsdlService);

		System.out.println("=== Services to be tested ===");
		for (Service service : TestServices.LIST) {
			System.out.println(service.getName() + " " + service.getUrl());
		}

		System.out.println("=== testing results");
		for (StatusDetails<Service> serviceStatusDetails : serviceStatus) {
			System.out.println(serviceStatusDetails.toString());
		}
	}
}