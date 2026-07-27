package com.bookservice.ws.impl;

import com.bookservice.ws.fault.WsException;
import com.bookservice.ws.fault.WsFault;
import com.bookservice.ws.handler.CredentialsHandler;
import com.bookservice.ws.model.BookType;
import com.bookservice.ws.model.GetBookInput;
import com.bookservice.ws.model.GetBookOutput;
import com.bookservice.ws.porttype.GetBookPortType;

import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Endpoint;
import java.util.GregorianCalendar;

// =============================================================================
// GetBookPortTypeImpl — SIB for the GetBook operation
// =============================================================================
// WHAT IS A SIB?
// SIB = Service Implementation Bean
//
// This is the actual business logic — the real code that runs when a client
// calls the GetBook SOAP operation. It implements the SEI (GetBookPortType).
//
// The relationship between SEI and SIB:
//
//   GetBookPortType (SEI/Interface)  ←  defines the CONTRACT (what operations exist)
//          ↑ implements
//   GetBookPortTypeImpl (SIB/Class)  ←  defines the BEHAVIOUR (what they actually do)
//
// This separation is intentional and good practice:
//   - The SEI is the public API. Clients and wsimport only need to know the SEI.
//   - The SIB is the private implementation. You can swap it without changing clients.
//   - The WSDL is generated/served from the SEI, not from this class.
// =============================================================================

@WebService(
    // ==========================================================================
    // @WebService on the IMPLEMENTATION CLASS
    // ==========================================================================
    // When @WebService appears on the implementation class (not the interface),
    // it needs extra attributes to link back to the SEI and control WSDL serving.

    // serviceName — maps to <wsdl:service name="GetBookService">
    // This is the top-level entry point clients look for in the WSDL.
    // The generated factory class (from wsimport) is also named after this:
    //   GetBookService.java → GetBookService.getGetBookPort()
    // ==========================================================================
    serviceName       = "GetBookService",

    // ==========================================================================
    // portName — maps to <wsdl:port name="GetBookPort">
    // A service can technically have multiple ports (different addresses/bindings).
    // We have one. The wsimport-generated factory method is named after this:
    //   getBookFactory.getGetBookPort()
    // ==========================================================================
    portName          = "GetBookPort",

    // ==========================================================================
    // endpointInterface — CRITICAL: links this SIB back to its SEI.
    // It tells JAX-WS: "use GetBookPortType (the interface) as the contract."
    // This ensures the WSDL is served from the clean interface, not this class.
    // ==========================================================================
    endpointInterface = "com.bookservice.ws.porttype.GetBookPortType",

    // ==========================================================================
    // targetNamespace — must match the WSDL targetNamespace
    // ==========================================================================
    targetNamespace   = "http://bookservice.com/getbook/wsdl/v1",

    // ==========================================================================
    // wsdlLocation — tells JAX-WS to use THIS WSDL instead of auto-generating one.
    // ==========================================================================
    // "classpath:wsdl/GetBook.wsdl" is a convention understood by the JAX-WS RI.
    // The file must be on the classpath at runtime (it is in src/main/resources/wsdl/).
    //
    // WHY use a pre-written WSDL instead of auto-generation?
    //   • The auto-generated WSDL may not perfectly match what clients expect
    //   • You want explicit control over namespace prefixes and element names
    //   • You are implementing against a WSDL given to you by a partner (contract-first)
    //   • The WSDL is shared across teams and must not change silently
    // ==========================================================================
    wsdlLocation      = "wsdl/GetBook.wsdl"
)

// =============================================================================
// @HandlerChain
// =============================================================================
// Wires the CredentialsHandler into the processing pipeline for this endpoint.
// The file "handler-chain.xml" on the classpath lists all handlers to apply.
// Handlers run in the order listed in the XML file.
//
// Alternative: You can register handlers programmatically via Endpoint API,
// but @HandlerChain keeps the configuration in a declarative file.
// =============================================================================
@HandlerChain(file = "/handler-chain.xml")
public class GetBookPortTypeImpl implements GetBookPortType {

    private final BookRepository repository = BookRepository.getInstance();

    @Override
    public GetBookOutput getBook(GetBookInput getBookInput)
            throws WsException {

        // =====================================================================
        // This is plain Java — no SOAP-specific code here at all.
        // JAX-WS handles all the XML marshalling/unmarshalling automatically:
        //
        //   Incoming SOAP XML:
        //     <bk:GetBookInput><bk:isbn>978-0-13-468599-1</bk:isbn></bk:GetBookInput>
        //         ↓  JAX-WS unmarshals (XML → Java)
        //   Java call:
        //     getBook(getBookInput)
        //         ↓  your code runs (repository lookup, validation, etc.)
        //   Java return:
        //     new GetBookOutput(book)
        //         ↓  JAX-WS marshals (Java → XML)
        //   Outgoing SOAP XML:
        //     <bk:GetBookOutput><bk:book>...</bk:book></bk:GetBookOutput>
        //
        // NOTE: By the time this method is called, CredentialsHandler has already
        // validated wsCredentials. If credentials were invalid, the handler would
        // have returned a SOAP Fault and this method would never be reached.
        // =====================================================================

        System.out.println("[GetBookPortTypeImpl] GetBook called. ISBN: " + getBookInput.getIsbn());

        // Input validation
        if (getBookInput.getIsbn() == null || getBookInput.getIsbn().trim().isEmpty()) {
            throw buildFault("INVALID_INPUT", "ISBN must not be empty.", "GetBook");
        }

        // Look up the book
        BookType book = repository.findByIsbn(getBookInput.getIsbn().trim());

        if (book == null) {
            // =====================================================================
            // Throwing WsException → SOAP Fault on the wire
            // =====================================================================
            // This becomes:
            //   <soapenv:Fault>
            //     <faultcode>soapenv:Server</faultcode>
            //     <faultstring>Book not found: 978-x</faultstring>
            //     <detail>
            //       <cmn:WsFault>
            //         <cmn:errorCode>BOOK_NOT_FOUND</cmn:errorCode>
            //         <cmn:errorMessage>No book found with ISBN: 978-x</cmn:errorMessage>
            //         <cmn:errorTimestamp>2024-01-15T10:30:00</cmn:errorTimestamp>
            //         <cmn:operationName>GetBook</cmn:operationName>
            //       </cmn:WsFault>
            //     </detail>
            //   </soapenv:Fault>
            // =====================================================================
            throw buildFault(
                "BOOK_NOT_FOUND",
                "No book found with ISBN: " + getBookInput.getIsbn(),
                "GetBook"
            );
        }

        // Success — wrap the book in GetBookOutput and return
        System.out.println("[GetBookPortTypeImpl] Found book: " + book.getTitle());
        return new GetBookOutput(book);
    }

    // =========================================================================
    // buildFault — helper to construct a WsException
    // =========================================================================
    // Centralises the boilerplate of creating the fault timestamp
    // and populating the WsFaultType bean.
    // =========================================================================
    private WsException buildFault(String errorCode, String errorMessage, String operationName)
            throws WsException {
        try {
            GregorianCalendar gc = new GregorianCalendar();
            XMLGregorianCalendar timestamp =
                DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);

            WsFault faultInfo = new WsFault(
                errorCode, errorMessage, timestamp, operationName);

            return new WsException(errorMessage, faultInfo);
        } catch (Exception e) {
            throw new WsException("Internal error building fault: " + e.getMessage(),
                new WsFault("INTERNAL_ERROR", e.getMessage(), null, operationName));
        }
    }

    // =========================================================================
    // publish — static method to start this endpoint
    // Called from ServerMain.
    // =========================================================================
    public static Endpoint publish(String address) {
        return Endpoint.publish(address, new GetBookPortTypeImpl());
    }
}
