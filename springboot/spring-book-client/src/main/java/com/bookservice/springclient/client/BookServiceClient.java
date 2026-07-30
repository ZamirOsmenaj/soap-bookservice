package com.bookservice.springclient.client;

import com.bookservice.springclient.callback.CredentialsCallback;
import com.bookservice.springclient.model.BookType;
import com.bookservice.springclient.model.CreateBookInput;
import com.bookservice.springclient.model.CreateBookOutput;
import com.bookservice.springclient.model.GetBookInput;
import com.bookservice.springclient.model.GetBookOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.math.BigDecimal;

// =============================================================================
// BookServiceClient — Spring-WS equivalent of BookClient (plain-Java version)
// =============================================================================
// Key differences from the plain-Java version:
//
//   Plain-Java:
//     GetBookService factory = new GetBookService(new URL(wsdlUrl));
//     GetBookPortType port    = factory.getGetBookPort();
//     ((BindingProvider) port).getRequestContext().put(ENDPOINT_ADDRESS, url);
//     port.getBook(input);   ← wsimport-generated proxy call
//
//   Spring-WS:
//     webServiceTemplate.marshalSendAndReceive(uri, request, credentialsCallback);
//     ← one call: marshals request, sends SOAP POST, unmarshals response
//     ← no wsimport, no stubs, no proxy objects
//     ← credentials injected via WebServiceMessageCallback (CredentialsCallback)
//
// SOAP faults come back as SoapFaultClientException instead of a typed
// WsException — Spring-WS wraps all server-side faults in this exception.
// The fault message and fault string are available via its accessors.
// =============================================================================
@Component
public class BookServiceClient {

    private static final String VALID_USER = "bookapp";
    private static final String VALID_PASS = "secret123";
    private static final String SYSTEM_ID  = "SPRING_BOOK_CLIENT";

    @Value("${server.host:spring-book-server}")
    private String serverHost;

    @Autowired
    private WebServiceTemplate webServiceTemplate;

    // =========================================================================
    // runDemo — executes the same 5-scenario demo as the plain-Java BookClient
    // =========================================================================
    public void runDemo() {
        String getBookUri    = "http://" + serverHost + ":8080/ws/getbook";
        String createBookUri = "http://" + serverHost + ":8080/ws/createbook";

        System.out.println("================================================");
        System.out.println(" Book Service SOAP Client  [Spring Boot / Spring-WS]");
        System.out.println("================================================");

        // ── Scenario 1: GetBook — existing ISBN (success) ────────────────────
        System.out.println("\n── Scenario 1: GetBook — existing ISBN ─────────");
        try {
            GetBookInput request = new GetBookInput("978-0-13-468599-1");
            GetBookOutput response = (GetBookOutput) webServiceTemplate.marshalSendAndReceive(
                    getBookUri, request,
                    new CredentialsCallback(VALID_USER, VALID_PASS, SYSTEM_ID));

            BookType book = response.getBook();
            System.out.println("  ✓ Found: " + book.getTitle());
            System.out.println("    Author : " + book.getAuthor());
            System.out.println("    Price  : $" + book.getPrice());
            System.out.println("    Stock  : " + book.getStockQuantity());
            System.out.println("    Genre  : " + book.getGenre());

        } catch (SoapFaultClientException e) {
            System.out.println("  ✗ SOAP Fault: " + e.getFaultStringOrReason());
        }

        // ── Scenario 2: GetBook — non-existent ISBN (expect BOOK_NOT_FOUND) ──
        System.out.println("\n── Scenario 2: GetBook — unknown ISBN ──────────");
        try {
            GetBookInput request = new GetBookInput("000-0-00-000000-0");
            webServiceTemplate.marshalSendAndReceive(
                    getBookUri, request,
                    new CredentialsCallback(VALID_USER, VALID_PASS, SYSTEM_ID));
            System.out.println("  ✗ Expected a fault but got a response!");

        } catch (SoapFaultClientException e) {
            System.out.println("  ✓ Got expected fault: " + e.getFaultStringOrReason());
        }

        // ── Scenario 3: GetBook — bad credentials (expect auth rejection) ────
        System.out.println("\n── Scenario 3: GetBook — bad credentials ───────");
        try {
            GetBookInput request = new GetBookInput("978-0-13-468599-1");
            webServiceTemplate.marshalSendAndReceive(
                    getBookUri, request,
                    new CredentialsCallback("wronguser", "wrongpass", SYSTEM_ID));
            System.out.println("  ✗ Expected an auth fault but got a response!");

        } catch (SoapFaultClientException e) {
            System.out.println("  ✓ Got expected auth fault: " + e.getFaultStringOrReason());
        } catch (Exception e) {
            System.out.println("  ✓ Got expected rejection: " + e.getMessage());
        }

        // ── Scenario 4: CreateBook — new book (success) ──────────────────────
        System.out.println("\n── Scenario 4: CreateBook — new book ───────────");
        try {
            BookType newBook = new BookType(
                "978-0-13-235088-4",
                "Clean Code",
                "Robert C. Martin",
                new BigDecimal("45.99"),
                20,
                "Programming"
            );
            CreateBookInput request = new CreateBookInput(newBook);
            CreateBookOutput response = (CreateBookOutput) webServiceTemplate.marshalSendAndReceive(
                    createBookUri, request,
                    new CredentialsCallback(VALID_USER, VALID_PASS, SYSTEM_ID));

            System.out.println("  ✓ Status  : " + response.getStatus());
            System.out.println("    Book ID : " + response.getBookId());
            System.out.println("    Message : " + response.getMessage());

        } catch (SoapFaultClientException e) {
            System.out.println("  ✗ SOAP Fault: " + e.getFaultStringOrReason());
        }

        // ── Scenario 5: CreateBook — duplicate ISBN (expect DUPLICATE status) ─
        System.out.println("\n── Scenario 5: CreateBook — duplicate ISBN ──────");
        try {
            // Attempt to create a book that already exists in the pre-populated catalog
            BookType duplicate = new BookType(
                "978-0-13-468599-1",   // same ISBN as Effective Java
                "Effective Java (2nd)",
                "Joshua Bloch",
                new BigDecimal("54.99"),
                5,
                "Programming"
            );
            CreateBookInput request = new CreateBookInput(duplicate);
            CreateBookOutput response = (CreateBookOutput) webServiceTemplate.marshalSendAndReceive(
                    createBookUri, request,
                    new CredentialsCallback(VALID_USER, VALID_PASS, SYSTEM_ID));

            System.out.println("  ✓ Status  : " + response.getStatus());
            System.out.println("    Message : " + response.getMessage());

        } catch (SoapFaultClientException e) {
            System.out.println("  ✗ SOAP Fault: " + e.getFaultStringOrReason());
        }

        System.out.println("\n================================================");
        System.out.println(" Demo complete.");
        System.out.println("================================================\n");
    }
}
