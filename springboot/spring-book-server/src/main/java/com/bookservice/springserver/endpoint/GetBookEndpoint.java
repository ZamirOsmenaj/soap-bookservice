package com.bookservice.springserver.endpoint;

import com.bookservice.springserver.fault.WsException;
import com.bookservice.springserver.model.BookType;
import com.bookservice.springserver.model.GetBookInput;
import com.bookservice.springserver.model.GetBookOutput;
import com.bookservice.springserver.model.WsFault;
import com.bookservice.springserver.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

// =============================================================================
// GetBookEndpoint — Spring-WS endpoint for the GetBook operation
// =============================================================================
// In the plain-Java version this was GetBookPortTypeImpl:
//   - @WebService(endpointInterface=..., wsdlLocation=..., serviceName=..., portName=...)
//   - implements GetBookPortType (the SEI interface)
//   - published via Endpoint.publish("http://0.0.0.0:8080/getbook", new GetBookPortTypeImpl())
//
// In Spring-WS:
//   @Endpoint   → marks this class as a Spring-WS endpoint (like @Controller for REST)
//               → Spring auto-discovers it via component scanning
//               → No Endpoint.publish() needed — MessageDispatcherServlet does this
//
//   @PayloadRoot → the routing annotation, equivalent to the URL path in REST.
//               → "when the SOAP Body contains a root element with this namespace
//                  and local name, dispatch to this method"
//               → namespace + localPart must match the XSD element definition:
//                   namespace="http://bookservice.com/book/v1"   (book.xsd targetNamespace)
//                   localPart="GetBookInput"                     (<xsd:element name="GetBookInput">)
//
//   @RequestPayload  → tells Spring-WS to unmarshal the SOAP Body into this parameter
//   @ResponsePayload → tells Spring-WS to marshal the return value into the SOAP Body
//
// The SEI interface (GetBookPortType) is gone entirely. In Spring-WS there is no
// separate interface/implementation split — the endpoint class IS the implementation,
// and the WSDL (served by WebServiceConfig) IS the contract.
// =============================================================================
@Endpoint
public class GetBookEndpoint {

    // Namespace and local part of the request payload root element
    // Must match exactly what is defined in book.xsd
    private static final String NAMESPACE_URI = "http://bookservice.com/book/v1";

    @Autowired
    private BookRepository repository;

    // =========================================================================
    // getBook — handles the GetBook SOAP operation
    // =========================================================================
    // Spring-WS unmarshals the SOAP Body into GetBookInput using JAXB,
    // calls this method, then marshals the returned GetBookOutput back to XML.
    //
    // Throwing WsException from here causes Spring-WS to produce a SOAP Fault
    // (because WsException is annotated with @SoapFault). The fault will have:
    //   <faultcode>soapenv:Server</faultcode>
    //   <faultstring>the exception message</faultstring>
    //
    // Note: the WsFault detail block is included automatically by Spring-WS
    // when it processes the @SoapFault annotation and serialises the exception.
    // =========================================================================
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetBookInput")
    @ResponsePayload
    public GetBookOutput getBook(@RequestPayload GetBookInput request) throws WsException {

        System.out.println("[GetBookEndpoint] GetBook called. ISBN: " + request.getIsbn());

        if (request.getIsbn() == null || request.getIsbn().trim().isEmpty()) {
            throw buildFault("INVALID_INPUT", "ISBN must not be empty.", "GetBook");
        }

        BookType book = repository.findByIsbn(request.getIsbn().trim());

        if (book == null) {
            throw buildFault(
                "BOOK_NOT_FOUND",
                "No book found with ISBN: " + request.getIsbn(),
                "GetBook"
            );
        }

        System.out.println("[GetBookEndpoint] Found book: " + book.getTitle());
        return new GetBookOutput(book);
    }

    // Builds a WsException with a populated WsFault detail bean
    private WsException buildFault(String errorCode, String errorMessage, String operationName)
            throws WsException {
        try {
            GregorianCalendar gc = new GregorianCalendar();
            XMLGregorianCalendar timestamp =
                    DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
            WsFault faultInfo = new WsFault(errorCode, errorMessage, timestamp, operationName);
            return new WsException(errorMessage, faultInfo);
        } catch (Exception e) {
            return new WsException("Internal error: " + e.getMessage(),
                    new WsFault("INTERNAL_ERROR", e.getMessage(), null, operationName));
        }
    }
}
