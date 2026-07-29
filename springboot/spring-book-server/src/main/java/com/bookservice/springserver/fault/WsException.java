package com.bookservice.springserver.fault;

import com.bookservice.springserver.model.WsFault;
import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

// =============================================================================
// WsException — business fault exception for Spring-WS
// =============================================================================
// In the plain-Java version this class used:
//   @WebFault(name="WsFault", faultBean="...", targetNamespace="...")
// and JAX-WS used getFaultInfo() to marshal the fault detail.
//
// In Spring-WS the mechanism differs slightly:
//   @SoapFault(faultCode = FaultCode.SERVER) tells Spring-WS to produce a
//   SOAP Fault with faultcode "Server" when this exception is thrown from
//   an @Endpoint method.
//
// The detailed WsFault bean is marshalled into the SOAP <detail> block by
// the GetBookEndpoint / CreateBookEndpoint classes using SoapFaultDefinition
// and SaajSoapFaultException directly, giving us full control over the
// fault detail XML (same as the plain-Java version).
// =============================================================================
@SoapFault(faultCode = FaultCode.SERVER)
public class WsException extends Exception {

    private static final long serialVersionUID = 1L;

    private final WsFault faultInfo;

    public WsException(String message, WsFault faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public WsException(String message, WsFault faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public WsFault getFaultInfo() {
        return faultInfo;
    }
}
