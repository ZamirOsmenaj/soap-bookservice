package com.bookservice.client;

// =============================================================================
// These imports come from the AUTO-GENERATED classes produced by wsimport.
// They are generated during 'mvn generate-sources' and placed in:
//   target/generated-sources/wsimport/
//
// wsimport reads TWO wsdl files and generates TWO sets of classes:
//
// From GetBook.wsdl:
//   com.bookservice.generated.getbook.GetBookService       ← factory
//   com.bookservice.generated.getbook.GetBookPortType      ← port interface
//   com.bookservice.generated.getbook.GetBookInput         ← request wrapper
//   com.bookservice.generated.getbook.GetBookOutput        ← response wrapper
//   com.bookservice.generated.getbook.WsException          ← fault exception
//   com.bookservice.generated.getbook.WsFault              ← fault data bean
//   com.bookservice.generated.getbook.WsCredentials        ← header bean
//   com.bookservice.generated.getbook.BookType             ← shared domain type
//   ... (plus ObjectFactory, package-info)
//
// From CreateBook.wsdl:
//   com.bookservice.generated.createbook.CreateBookService
//   com.bookservice.generated.createbook.CreateBookPortType
//   com.bookservice.generated.createbook.CreateBookInput
//   com.bookservice.generated.createbook.CreateBookOutput
//   com.bookservice.generated.createbook.WsException
//   com.bookservice.generated.createbook.WsFault
//   com.bookservice.generated.createbook.WsCredentials
//   com.bookservice.generated.createbook.BookType
//   ...
//
// Notice that common types like BookType, WsCredentials, WsFault are generated
// ONCE PER PACKAGE — each wsimport invocation generates its own copy.
// This is a known limitation of wsimport. In enterprise projects a JAXB binding
// customisation (bindings.xml) is used to share a single set of common classes.
// For this demo, two separate packages keeps things clear.
// =============================================================================
import com.bookservice.generated.getbook.GetBookPortType;
import com.bookservice.generated.getbook.GetBookService;
import com.bookservice.generated.getbook.GetBookInput;
import com.bookservice.generated.getbook.GetBookOutput;

import com.bookservice.generated.createbook.CreateBookService;
import com.bookservice.generated.createbook.CreateBookPortType;
import com.bookservice.generated.createbook.CreateBookInput;
import com.bookservice.generated.createbook.CreateBookOutput;
import com.bookservice.generated.createbook.BookType;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.handler.Handler;

public class BookClient {

    // The valid credentials the server expects (defined in CredentialsHandler)
    private static final String VALID_USER = "bookapp";
    private static final String VALID_PASS = "secret123";
    private static final String SYSTEM_ID  = "BOOK_CLIENT_DEMO";

    private static final String CMN_NS = "http://bookservice.com/common/v1";

    public static void main(String[] args) throws Exception {

        String serverHost = System.getProperty("server.host", "book-server");

        String getBookWsdlUrl     = "http://" + serverHost + ":8080/getbook?wsdl";
        String createBookWsdlUrl  = "http://" + serverHost + ":8080/createbook?wsdl";
        String getBookEndpoint    = "http://" + serverHost + ":8080/getbook";
        String createBookEndpoint = "http://" + serverHost + ":8080/createbook";

        System.out.println("================================================");
        System.out.println(" Book Service SOAP Client");
        System.out.println("================================================");

        waitForServer(getBookWsdlUrl);

        // =====================================================================
        // PART 1: GetBook — auto-generated client stubs
        // =====================================================================
        System.out.println("\n── GetBook Service ──────────────────────────────");

        // Create the service factory from the WSDL
        GetBookService getBookFactory = new GetBookService(new URL(getBookWsdlUrl));

        // Get the port (the proxy object that makes actual SOAP calls)
        GetBookPortType getBookPort   = getBookFactory.getGetBookPort();

        // =====================================================================
        // Override the endpoint address at RUNTIME
        // =====================================================================
        // WHY? The WSDL's <soap:address> contains "http://book-server:8080/getbook".
        // Inside Docker that is correct. But if running locally (not in Docker),
        // you might want "http://localhost:8080/getbook".
        //
        // BindingProvider lets you override the endpoint address programmatically.
        // This is a common pattern in enterprise code: the factory reads the WSDL
        // to build the stubs, but the endpoint URL comes from configuration.
        // =====================================================================
        ((BindingProvider) getBookPort).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, getBookEndpoint);

        attachCredentialsHandler(getBookPort, VALID_USER, VALID_PASS, SYSTEM_ID);

        // ── Demo 1a: GetBook — existing book ────────────────────────────────
        System.out.println("\n[1a] GetBook - existing book (ISBN: 978-0-13-468599-1)");
        try {
            GetBookInput input = new GetBookInput();
            input.setIsbn("978-0-13-468599-1");

            GetBookOutput output = getBookPort.getBook(input);
            printBook(output.getBook());

        } catch (com.bookservice.generated.getbook.WsException e) {
            System.err.println("SOAP Fault    : " + e.getMessage());
            System.err.println("Error code    : " + e.getFaultInfo().getErrorCode());
        }

        // ── Demo 1b: GetBook — book does not exist (expect BOOK_NOT_FOUND fault)
        System.out.println("\n[1b] GetBook - non-existent book (ISBN: 000-0-00-000000-0)");
        try {
            GetBookInput input = new GetBookInput();
            input.setIsbn("000-0-00-000000-0");
            getBookPort.getBook(input);

        } catch (com.bookservice.generated.getbook.WsException e) {
            // =================================================================
            // WsException catch — handling a SOAP Fault
            // =================================================================
            // When the server throws a WsException, JAX-WS serializes it as
            // a SOAP Fault on the wire. On the client side, the generated stub
            // deserializes the SOAP Fault back into a WsException.
            //
            // e.getMessage()          → the SOAP <faultstring>
            // e.getFaultInfo()        → the WsFault bean from the <detail> block
            // e.getFaultInfo().getErrorCode() → the machine-readable error code
            // =================================================================
            System.out.println("Expected fault received:");
            System.out.println("  faultstring : " + e.getMessage());
            System.out.println("  errorCode   : " + e.getFaultInfo().getErrorCode());
            System.out.println("  errorMessage: " + e.getFaultInfo().getErrorMessage());
            System.out.println("  operation   : " + e.getFaultInfo().getOperationName());
        }

        // ── Demo 1c: GetBook — wrong credentials (expect auth rejection) ─────
        System.out.println("\n[1c] GetBook - invalid credentials (rejected by server handler)");
        try {
            // Swap in bad credentials on the same port by attaching a new handler
            attachCredentialsHandler(getBookPort, "hacker", "wrongpass", "UNKNOWN");

            GetBookInput input = new GetBookInput();
            input.setIsbn("978-0-13-468599-1");
            getBookPort.getBook(input);

        } catch (WebServiceException e) {
            // CredentialsHandler returned a SOAP Fault — arrives as WebServiceException
            System.out.println("Auth rejected (expected): " + e.getMessage());
        } catch (com.bookservice.generated.getbook.WsException e) {
            System.out.println("Auth fault: " + e.getMessage());
        } finally {
            // Restore valid credentials for any further calls on this port
            attachCredentialsHandler(getBookPort, VALID_USER, VALID_PASS, SYSTEM_ID);
        }

        // =====================================================================
        // PART 2: CreateBook — auto-generated client stubs
        // =====================================================================
        System.out.println("\n── CreateBook Service ───────────────────────────");

        CreateBookService createBookFactory = new CreateBookService(new URL(createBookWsdlUrl));
        CreateBookPortType createBookPort   = createBookFactory.getCreateBookPort();

        ((BindingProvider) createBookPort).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, createBookEndpoint);

        // Attach credentials handler to the CreateBook port too
        attachCredentialsHandler(createBookPort, VALID_USER, VALID_PASS, SYSTEM_ID);

        // ── Demo 2a: CreateBook — new book ───────────────────────────────────
        System.out.println("\n[2a] CreateBook - new book");
        try {
            BookType newBook = new BookType();
            newBook.setIsbn("978-0-13-110362-7-NEW");
            newBook.setTitle("Clean Code");
            newBook.setAuthor("Robert C. Martin");
            newBook.setPrice(new BigDecimal("44.99"));
            newBook.setStockQuantity(25);
            newBook.setGenre("Programming");

            CreateBookInput input = new CreateBookInput();
            input.setBook(newBook);

            CreateBookOutput output = createBookPort.createBook(input);
            System.out.println("Status : " + output.getStatus());
            System.out.println("Book ID: " + output.getBookId());
            System.out.println("Message: " + output.getMessage());

        } catch (com.bookservice.generated.createbook.WsException e) {
            System.err.println("Fault: " + e.getMessage());
        }

        // ── Demo 2b: CreateBook — duplicate ISBN ─────────────────────────────
        System.out.println("\n[2b] CreateBook - duplicate ISBN (978-0-13-468599-1)");
        try {
            BookType duplicate = new BookType();
            duplicate.setIsbn("978-0-13-468599-1");
            duplicate.setTitle("Effective Java Duplicate");
            duplicate.setAuthor("Joshua Bloch");
            duplicate.setPrice(new BigDecimal("49.99"));
            duplicate.setStockQuantity(5);

            CreateBookInput input = new CreateBookInput();
            input.setBook(duplicate);

            CreateBookOutput output = createBookPort.createBook(input);
            System.out.println("Status : " + output.getStatus());
            System.out.println("Message: " + output.getMessage());

        } catch (com.bookservice.generated.createbook.WsException e) {
            System.err.println("Fault: " + e.getMessage());
        }

        System.out.println("\n================================================");
        System.out.println(" All demos completed.");
        System.out.println("================================================");
    }

    // TODO: Missing explanation comment
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void attachCredentialsHandler(Object port,
                                                  String username,
                                                  String password,
                                                  String systemId) {
        BindingProvider bp = (BindingProvider) port;
        List<Handler> chain = new ArrayList<Handler>();
        chain.add(new CredentialsInjector(username, password, systemId));
        bp.getBinding().setHandlerChain(chain);
    }

    // TODO: Missing explanation comment
    private static class CredentialsInjector implements SOAPHandler<SOAPMessageContext> {

        private final String username;
        private final String password;
        private final String systemId;

        CredentialsInjector(String username, String password, String systemId) {
            this.username = username;
            this.password = password;
            this.systemId = systemId;
        }

        @Override
        public boolean handleMessage(SOAPMessageContext ctx) {
            Boolean outbound = (Boolean) ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            if (!Boolean.TRUE.equals(outbound)) {
                return true; // inbound response — nothing to do
            }

            try {
                SOAPMessage  msg      = ctx.getMessage();
                SOAPEnvelope envelope = msg.getSOAPPart().getEnvelope();

                SOAPHeader header = envelope.getHeader();
                if (header == null) {
                    header = envelope.addHeader();
                }

                QName credQName = new QName(CMN_NS, "WsCredentials", "cmn");
                SOAPHeaderElement credEl = header.addHeaderElement(credQName);

                SOAPElement userEl = credEl.addChildElement(
                        new QName(CMN_NS, "username", "cmn"));
                userEl.setTextContent(username);

                SOAPElement passEl = credEl.addChildElement(
                        new QName(CMN_NS, "password", "cmn"));
                passEl.setTextContent(password);

                SOAPElement sysEl = credEl.addChildElement(
                        new QName(CMN_NS, "systemId", "cmn"));
                sysEl.setTextContent(systemId);

                msg.saveChanges();

            } catch (Exception e) {
                throw new WebServiceException("Failed to inject credentials header: "
                        + e.getMessage(), e);
            }
            return true;
        }

        @Override
        public boolean handleFault(SOAPMessageContext ctx) { return true; }

        @Override
        public void close(MessageContext ctx) {}

        @Override
        public java.util.Set<QName> getHeaders() { return null; }
    }

    // TODO: Missing explanation comment
    private static void printBook(com.bookservice.generated.getbook.BookType b) {
        System.out.println("  ISBN    : " + b.getIsbn());
        System.out.println("  Title   : " + b.getTitle());
        System.out.println("  Author  : " + b.getAuthor());
        System.out.println("  Price   : " + b.getPrice());
        System.out.println("  Stock   : " + b.getStockQuantity());
        System.out.println("  Genre   : " + b.getGenre());
    }

    // TODO: Missing explanation comment
    private static void waitForServer(String wsdlUrl) {
        System.out.println("Waiting for server at: " + wsdlUrl);
        for (int i = 1; i <= 20; i++) {
            try {
                new URL(wsdlUrl).openStream().close();
                System.out.println("Server ready!\n");
                return;
            } catch (Exception e) {
                System.out.println("  Attempt " + i + "/20...");
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
        throw new WebServiceException("Server did not start in time.");
    }

}
