package eu.heliovo.monitoring.model;

import eu.heliovo.monitoring.statics.Services;

/**
 * Represents a test for service operations, executed in testing stage. Please see {@link TestingServiceImpl} and
 * {@link Services}.
 * 
 * @author Kevin Seidler
 * 
 */
public interface OperationTest {

	String getOperationName();

	String getRequestContent();

	String getResponseContent();

}