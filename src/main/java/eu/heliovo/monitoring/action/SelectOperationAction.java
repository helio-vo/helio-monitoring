package eu.heliovo.monitoring.action;

import com.eviware.soapui.impl.wsdl.*;

/**
 * Selects an operation to call on the target web service.
 * 
 * @author Kevin Seidler
 * 
 */
public final class SelectOperationAction implements Action<WsdlOperation> {

	private final WsdlInterface wsdlInterface;

	public SelectOperationAction(WsdlInterface wsdlInterface) {
		this.wsdlInterface = wsdlInterface;
	}

	@Override
	public WsdlOperation getResult() throws Exception {
		return wsdlInterface.getOperationAt(0);
	}

}