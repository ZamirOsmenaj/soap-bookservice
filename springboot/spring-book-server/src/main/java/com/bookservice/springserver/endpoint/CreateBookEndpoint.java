package com.bookservice.springserver.endpoint;

import com.bookservice.springserver.fault.WsException;
import com.bookservice.springserver.model.BookType;
import com.bookservice.springserver.model.CreateBookInput;
import com.bookservice.springserver.model.CreateBookOutput;
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
// CreateBookEndpoint — Spring-WS endpoint for the CreateBook operation
// =============================================================================
// Same structure as GetBookEndpoint. See that class for the detailed breakdown
// of @Endpoint, @PayloadRoot, @RequestPayload, and @ResponsePayload.
//
// This replaces CreateBookPortTypeImpl from the plain-Java version.
// The @Autowired BookRepository is the same singleton shared with GetBookEndpoint,
// guaranteeing a consistent in-memory catalog across both operations.
// =============================================================================
@Endpoint
public class CreateBookEndpoint {

    private static final String NAMESPACE_URI = "http://bookservice.com/book/v1";

    @Autowired
    private BookRepository repository;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CreateBookInput")
    @ResponsePayload
    public CreateBookOutput createBook(@RequestPayload CreateBookInput request) throws WsException {

        System.out.println("[CreateBookEndpoint] CreateBook called.");

        // Input validation
        if (request == null || request.getBook() == null) {
            throw buildFault("INVALID_INPUT", "CreateBookInput and book must not be null.", "CreateBook");
        }

        BookType book = request.getBook();

        if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            throw buildFault("INVALID_INPUT", "Book ISBN must not be empty.", "CreateBook");
        }
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw buildFault("INVALID_INPUT", "Book title must not be empty.", "CreateBook");
        }

        // Duplicate check — return a DUPLICATE status instead of a fault
        if (repository.existsByIsbn(book.getIsbn().trim())) {
            System.out.println("[CreateBookEndpoint] Duplicate ISBN: " + book.getIsbn());
            return new CreateBookOutput(
                "EXISTING",
                "DUPLICATE",
                "A book with ISBN " + book.getIsbn() + " already exists."
            );
        }

        // Save the book and return success
        String bookId = repository.generateBookId();
        repository.addBook(book);

        System.out.println("[CreateBookEndpoint] Created book: " + book.getTitle() + " → ID: " + bookId);

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
            return new WsException("Internal error: " + e.getMessage(),
                    new WsFault("INTERNAL_ERROR", e.getMessage(), null, operationName));
        }
    }
}
