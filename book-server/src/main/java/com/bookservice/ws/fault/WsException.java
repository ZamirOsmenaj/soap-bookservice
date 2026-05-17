package com.bookservice.ws.fault;

import javax.xml.ws.WebFault;

// =============================================================================
// WHAT IS THIS CLASS?
// =============================================================================
// WsException is the Java exception that the service operations declare
// in their throws clause. When thrown, JAX-WS converts it into a SOAP Fault.
//
// JAVA THROWS CLAUSE  →  WSDL FAULT  →  SOAP FAULT ON THE WIRE
//
//   Java:  throw new WsException("Book not found", faultInfo);
//     ↓
//   WSDL:  <wsdl:fault message="tns:WsException" name="WsException"/>
//     ↓
//   SOAP wire:
//     <soapenv:Fault>
//       <faultcode>soapenv:Server</faultcode>
//       <faultstring>Book not found</faultstring>
//       <detail>
//         <cmn:WsFault>
//           <cmn:errorCode>BOOK_NOT_FOUND</cmn:errorCode>
//           ...
//         </cmn:WsFault>
//       </detail>
//     </soapenv:Fault>
//
// And on the CLIENT side, the generated stub RECEIVES this SOAP Fault and
// re-throws it as a WsException (or a generated equivalent class).
// =============================================================================

// =============================================================================
// @WebFault annotation
// =============================================================================
// This annotation tells JAX-WS:
//   1. This exception represents a declared WSDL fault (not a system error)
//   2. How to serialize the fault info into the SOAP <detail> block
//
// name="WsException"
//   → The name of the fault element in the SOAP <detail>.
//     Must match the fault name in the WSDL binding:
//       <wsdl:fault name="WsException"> and <soap:fault name="WsException"/>
//
// faultBean="com.bookservice.ws.fault.WsFault"
//   → The class that carries the detailed fault data.
//     JAX-WS calls getFaultInfo() on this exception to get the WsFault
//     object, then marshals it into the SOAP <detail> XML.
//
// targetNamespace="http://bookservice.com/common/v1"
//   → The XML namespace for the fault element.
//     Must match the namespace of cmn:WsFault in common.xsd.
// =============================================================================
@WebFault(
    name = "WsFault",
    faultBean = "com.bookservice.ws.fault.WsFault",
    targetNamespace = "http://bookservice.com/common/v1"
)
public class WsException extends Exception {

    // =========================================================================
    // serialVersionUID
    // =========================================================================
    // Required for all Serializable classes (Exception implements Serializable).
    // Used by Java's serialization mechanism to verify class compatibility.
    // Any constant long value is fine — 1L is a common convention.
    // =========================================================================
    private static final long serialVersionUID = 1L;

    // =========================================================================
    // faultInfo
    // =========================================================================
    // This field holds the structured fault data (WsFault).
    // JAX-WS REQUIRES this field AND a getFaultInfo() method.
    // This is a JAX-WS contract: the runtime calls getFaultInfo()
    // to get the object to marshal into the SOAP <detail> block.
    // =========================================================================
    private WsFault faultInfo;

    // =========================================================================
    // Constructors
    // =========================================================================
    // JAX-WS also REQUIRES these two specific constructors:
    //   1. WsException(String message, FaultBean faultInfo)
    //   2. WsException(String message, FaultBean faultInfo, Throwable cause)
    //
    // The "message" string becomes the SOAP <faultstring> element.
    // The "faultInfo" object becomes the SOAP <detail> block content.
    // =========================================================================
    public WsException(String message, WsFault faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public WsException(String message, WsFault faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    // =========================================================================
    // getFaultInfo() — REQUIRED by JAX-WS
    // =========================================================================
    // JAX-WS uses reflection to find and call this method.
    // The returned WsFault object is marshalled into the SOAP <detail>.
    // =========================================================================
    public WsFault getFaultInfo() {
        return this.faultInfo;
    }
}
