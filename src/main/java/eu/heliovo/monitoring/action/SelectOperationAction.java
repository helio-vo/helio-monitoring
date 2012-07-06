package eu.heliovo.monitoring.action;

import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlOperation;

/**
 * Selects an operation to call on the target web service. It looks for an operation called getStatus. 
 * If not found it always selects the first one, because this is
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
	public WsdlOperation getResult() {
	    WsdlOperation monitorOperation = wsdlInterface.getOperationByName("getStatus");
	    if (monitorOperation == null) {
	        monitorOperation = wsdlInterface.getOperationAt(0);
	    }
		return monitorOperation;
	}
}