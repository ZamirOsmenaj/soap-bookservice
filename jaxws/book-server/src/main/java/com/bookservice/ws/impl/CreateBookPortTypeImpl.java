package com.bookservice.ws.impl;

import com.bookservice.ws.fault.WsException;
import com.bookservice.ws.fault.WsFault;
import com.bookservice.ws.model.BookType;
import com.bookservice.ws.model.CreateBookInput;
import com.bookservice.ws.model.CreateBookOutput;
import com.bookservice.ws.porttype.CreateBookPortType;

import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Endpoint;
import java.util.GregorianCalendar;

// =============================================================================
// CreateBookPortTypeImpl — SIB for the CreateBook operation
// =============================================================================
// This is the Service Implementation Bean (SIB).
// It implements the SEI (CreateBookPortType) and contains the business logic.
// Pattern is identical to GetBookPortTypeImpl — consistent structure throughout.
//
// For the full educational breakdown of the SEI/SIB relationship, @WebService
// on the implementation class, and the marshalling/unmarshalling flow —
// see GetBookPortTypeImpl.java. This file intentionally stays lean.
// =============================================================================

@WebService(
    serviceName       = "CreateBookService",
    portName          = "CreateBookPort",
    endpointInterface = "com.bookservice.ws.porttype.CreateBookPortType",
    targetNamespace   = "http://bookservice.com/createbook/wsdl/v1",
    wsdlLocation      = "wsdl/CreateBook.wsdl"
)

@HandlerChain(file = "/handler-chain.xml")
public class CreateBookPortTypeImpl implements CreateBookPortType {

    private final BookRepository repository = BookRepository.getInstance();

    @Override
    public CreateBookOutput createBook(CreateBookInput createBookInput)
            throws WsException {

        // =====================================================================
        // NOTE: By the time this method is called, CredentialsHandler has already
        // validated wsCredentials. If credentials were invalid, the handler would
        // have returned a SOAP Fault and this method would never be reached.
        // =====================================================================

        System.out.println("[CreateBookPortTypeImpl] CreateBook called.");

        // Input validation
        if (createBookInput == null || createBookInput.getBook() == null) {
            throw buildFault("INVALID_INPUT", "CreateBookInput and book must not be null.", "CreateBook");
        }

        BookType book = createBookInput.getBook();

        if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            throw buildFault("INVALID_INPUT", "Book ISBN must not be empty.", "CreateBook");
        }
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw buildFault("INVALID_INPUT", "Book title must not be empty.", "CreateBook");
        }

        // Check for duplicate
        if (repository.existsByIsbn(book.getIsbn().trim())) {
            // =================================================================
            // DUPLICATE scenario — we return a CREATED output with status=DUPLICATE
            // instead of throwing a fault. This is a business decision:
            //   Option A: return a fault   → caller knows strictly this is an error
            //   Option B: return DUPLICATE status → caller can handle gracefully
            // We chose Option B here for illustration.
            // In a strict contract you might throw instead.
            // =================================================================
            System.out.println("[CreateBookPortTypeImpl] Duplicate ISBN: " + book.getIsbn());
            return new CreateBookOutput(
                "EXISTING",
                "DUPLICATE",
                "A book with ISBN " + book.getIsbn() + " already exists."
            );
        }

        // Save the book
        String bookId = repository.generateBookId();
        repository.addBook(book);

        System.out.println("[CreateBookPortTypeImpl] Created book: " + book.getTitle() + " → ID: " + bookId);

        return new CreateBookOutput(
            bookId,
            "CREATED",
            "Book '" + book.getTitle() + "' successfully registered with ID " + bookId
        );
    }

    private WsException buildFault(String errorCode, String errorMessage, String operationName)
            throws WsException {
        try {
            GregorianCalendar gc = new GregorianCalendar();
            XMLGregorianCalendar timestamp =
                DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
            WsFault faultInfo = new WsFault(errorCode, errorMessage, timestamp, operationName);
            return new WsException(errorMessage, faultInfo);
        } catch (Exception e) {
            throw new WsException("Internal error building fault: " + e.getMessage(),
                new WsFault("INTERNAL_ERROR", e.getMessage(), null, operationName));
        }
    }

    public static Endpoint publish(String address) {
        return Endpoint.publish(address, new CreateBookPortTypeImpl());
    }
}
