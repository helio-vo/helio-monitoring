package eu.heliovo.monitoring.model;

import java.util.List;

/**
 * Service with predefinitions (request, response) to be monitored.
 * 
 * @author Kevin Seidler
 * 
 */
public interface TestingService extends Service {

	List<OperationTest> getOperationTests();
}