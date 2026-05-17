package com.bookservice.ws.porttype;

import com.bookservice.ws.fault.WsException;
import com.bookservice.ws.model.GetBookInput;
import com.bookservice.ws.model.GetBookOutput;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

// =============================================================================
// WHAT IS A SEI?
// =============================================================================
// SEI = Service Endpoint Interface
//
// This Java interface IS the contract of your SOAP service.
// It is the Java mirror of <wsdl:portType name="GetBookPortType"> in GetBook.wsdl.
//
// Think of it like this:
//   Java Interface + @WebService annotations  ←→  WSDL <portType>
//
// In this project we use the CONTRACT-FIRST approach:
//   The WSDL is written by hand first, then this SEI is written to match it.
//   The server uses wsdlLocation to serve the pre-written WSDL as-is.
//
// In code-first the flow is reversed:
//   Java Interface + annotations  →  JAX-WS auto-generates the WSDL
//
// Either way, the SEI is the single source of truth for what operations exist.
//
// NAME CONVENTION:
//   Interface name: GetBookPortType
//   → Ends with "PortType" to clearly signal this is a WSDL portType mapping.
//   → In simpler demos you might see "{OperationName}Service" — fine for small examples,
//     but real-world enterprise services always use the PortType suffix so you
//     can immediately tell: "this is the SEI, not a utility or model class."
// =============================================================================

// =============================================================================
// @WebService on the SEI (interface)
// =============================================================================
// Marks this interface as a SOAP Web Service definition.
//
// name="GetBookPortType"
//   → This becomes the <wsdl:portType name="GetBookPortType"> element.
//     The portType is the ABSTRACT interface — the list of operations available,
//     without any transport or encoding details.
//     Think of it as: "what can I call?" — not "how do I call it?"
//
// targetNamespace="http://bookservice.com/getbook/wsdl/v1"
//   → This is an XML Namespace — it is NOT a real URL you visit in a browser!
//     XML Namespaces look like URLs but they are just unique string identifiers
//     used to avoid naming collisions between XML elements from different sources.
//
//     Example problem without namespaces:
//       Two companies both define an XML element called <order>.
//       They mean completely different things. How does a parser know which is which?
//     Solution: each company prefixes with their namespace:
//       <company1:order xmlns:company1="http://company1.com/"/>
//       <company2:order xmlns:company2="http://company2.com/"/>
//
//     In WSDL and SOAP, the targetNamespace groups all your service's XML elements
//     under one unique identifier. It must be unique — by convention it looks like
//     a URL, but http://bookservice.com/getbook/wsdl/v1 doesn't have to be reachable.
//
//     In the WSDL you will see it referenced as:
//       xmlns:tns="http://bookservice.com/getbook/wsdl/v1"  (tns = "this namespace")
//     And elements defined by this WSDL are prefixed with tns:
//       tns:GetBookRequest, tns:GetBookPortType, tns:GetBookBinding, etc.
// =============================================================================
@WebService(
    name            = "GetBookPortType", // → <wsdl:portType name="GetBookPortType">
    targetNamespace = "http://bookservice.com/getbook/wsdl/v1" // → WSDL targetNamespace
)

// =============================================================================
// @SOAPBinding
// =============================================================================
// Controls how Java method calls are translated into SOAP XML messages.
//
// There are two styles:
//
// Style.RPC  (older, simpler)
//   The method name wraps the parameters directly in the SOAP body.
//   Less flexible, parameters are loosely typed.
//   Rarely used in modern services.
//
// Style.DOCUMENT  (modern standard — what we use)
//   The entire request body is a single well-defined XML document.
//   The shape of that document is formally defined in the XSD schema
//   (in our case, in book.xsd and common.xsd).
//   More verbose but strongly typed and tool-friendly.
//   THIS is what enterprise SOAP services use.
//
// use=LITERAL (the default with DOCUMENT style)
//   The XML is written "as-is" (literal) from the schema.
//   The alternative ENCODED is deprecated and not used in modern services.
// =============================================================================
@SOAPBinding(
    style           = SOAPBinding.Style.DOCUMENT,
    use             = SOAPBinding.Use.LITERAL,
    parameterStyle  = SOAPBinding.ParameterStyle.BARE
)

// =============================================================================
// @XmlSeeAlso
// =============================================================================
// Tells JAXB: "when marshalling/unmarshalling for this service,
// also include these classes in the JAXBContext."
//
// WHY is this needed?
// JAX-WS auto-discovers types from method signatures. But if a type is only
// referenced indirectly (e.g. BookType inside GetBookOutput) the JAXBContext
// may not include it. @XmlSeeAlso ensures all relevant types are registered.
//
// In practice: list all Input, Output, and fault bean classes used.
// =============================================================================
@XmlSeeAlso({ GetBookInput.class, GetBookOutput.class })
public interface GetBookPortType {

    // =========================================================================
    // GetBook operation
    // =========================================================================

    // =========================================================================
    // @WebMethod
    // =========================================================================
    // Marks this Java method as a SOAP operation.
    //
    // operationName="GetBook"
    //   → This name appears in the WSDL in two places:
    //     1. <wsdl:portType>: <wsdl:operation name="GetBook"> — the abstract def
    //     2. <wsdl:binding>:  <wsdl:operation name="GetBook"> — the SOAP mapping
    //   → It also becomes the SOAPAction HTTP header value (see GetBook.wsdl).
    // =========================================================================
    @WebMethod(
        operationName = "GetBook", // → <wsdl:operation name="GetBook">
        action = "GetBook"
    )

    // =========================================================================
    // @WebResult
    // =========================================================================
    // Controls the XML element name for the return value in the response message.
    //
    // name = "GetBookOutput"
    //   → The wrapper element name in the response body.
    //     Matches the element name in book.xsd.
    //     Without this annotation the element would be named "return" (ugly default).
    //
    // targetNamespace = "http://bookservice.com/book/v1"
    //   → The namespace of the response element. Must match book.xsd's
    //     targetNamespace (not the WSDL namespace).
    //
    // partName = "GetBookOutput"
    //   → Matches <wsdl:part name="GetBookOutput"> in the message.
    // =========================================================================
    @WebResult(
        name            = "GetBookOutput",
        targetNamespace = "http://bookservice.com/getbook/v1",
        partName        = "GetBookOutput"
    )
    GetBookOutput getBook(

        // =====================================================================
        // @WebParam for the request body
        // =====================================================================
        // name = "GetBookInput"
        //   → The XML element name in the SOAP body for this parameter.
        //     Matches <wsdl:part name="GetBookInput"> in the request message.
        //     Without this annotation the element would be named "arg0" (ugly default).
        //
        // targetNamespace = "http://bookservice.com/book/v1"
        //   → Namespace of GetBookInput element (from book.xsd).
        //
        // mode = WebParam.Mode.IN
        //   → This parameter is input-only (the client sends it to the server).
        //     Other modes:
        //       OUT   → server sends it to the client (maps to Holder<T>)
        //       INOUT → bidirectional (also maps to Holder<T>)
        //     INOUT is used for in/out parameters — rare in document/literal style.
        // =====================================================================
        @WebParam(
            name            = "GetBookInput",
            targetNamespace = "http://bookservice.com/getbook/v1",
            partName        = "GetBookInput",
            mode            = WebParam.Mode.IN
        )
        GetBookInput getBookInput

    ) throws WsException;
}

