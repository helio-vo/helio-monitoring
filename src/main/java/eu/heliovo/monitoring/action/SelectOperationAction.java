package eu.heliovo.monitoring.action;

import com.eviware.soapui.impl.wsdl.*;

/**
 * Selects an operation to call on the target web service. It always selects the first one, because this is
 * deterministic and there should always be a first operation.
 * 
 * @author Kevin Seidler
 * 
 */
public final class SelectOperationAction implements ResultAction<WsdlOperation> {

	private final WsdlInterface wsdlInterface;

	public SelectOperationAction(WsdlInterface wsdlInterface) {
		this.wsdlInterface = wsdlInterface;
	}

	@Override
	public WsdlOperation getResult() throws Exception {
		return wsdlInterface.getOperationAt(0);
	}

}