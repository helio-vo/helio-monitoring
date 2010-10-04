package eu.heliovo.monitoring.model;


public class OperationTestImpl implements OperationTest {

	private final String operationName;
	private final String requestContent;
	private final String responseContent;

	protected OperationTestImpl(final String operationName, final String requestContent, final String responseContent) {
		this.operationName = operationName;
		this.requestContent = requestContent;
		this.responseContent = responseContent;
	}

	public final String getOperationName() {
		return operationName;
	}

	public final String getRequestContent() {
		return requestContent;
	}

	public final String getResponseContent() {
		return responseContent;
	}

	@Override
	public String toString() {
		return "OperationTestImpl [operationName=" + operationName + ", requestContent=" + requestContent
				+ ", responseContent=" + responseContent + "]";
	}

}
