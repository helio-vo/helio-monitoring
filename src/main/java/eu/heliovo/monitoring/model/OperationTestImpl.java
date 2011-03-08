package eu.heliovo.monitoring.model;

/**
 * Please see {@link OperationTest}.
 * 
 * @author Kevin Seidler
 * 
 */
public final class OperationTestImpl implements OperationTest {

	private final String operationName;
	private final String requestContent;
	private final String responseContent;

	protected OperationTestImpl(final String operationName, final String requestContent, final String responseContent) {
		this.operationName = operationName;
		this.requestContent = requestContent;
		this.responseContent = responseContent;
	}

	@Override
	public String getOperationName() {
		return operationName;
	}

	@Override
	public String getRequestContent() {
		return requestContent;
	}

	@Override
	public String getResponseContent() {
		return responseContent;
	}

	@Override
	public String toString() {
		return "OperationTestImpl [operationName=" + operationName + ", requestContent=" + requestContent
				+ ", responseContent=" + responseContent + "]";
	}

}