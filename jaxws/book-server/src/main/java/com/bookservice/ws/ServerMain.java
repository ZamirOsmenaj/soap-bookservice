package com.bookservice.ws;

import com.bookservice.ws.impl.CreateBookPortTypeImpl;
import com.bookservice.ws.impl.GetBookPortTypeImpl;

import javax.xml.ws.Endpoint;
import java.util.Arrays;

// =============================================================================
// ServerMain — entry point that publishes both SOAP endpoints
// =============================================================================
// This class does exactly one thing: start the HTTP server and register
// the two service implementations at their respective URL paths.
//
// We use javax.xml.ws.Endpoint — built into Java 8's JDK.
// It starts a lightweight HTTP server internally (com.sun.net.httpserver).
// No Tomcat, no Jetty, no servlet container needed.
//
// In a real project each operation might be its own microservice.
// Here we publish both from one JVM to keep the demo simple.
//
// Each endpoint gets its own URL path:
//   http://0.0.0.0:8080/getbook    → GetBook operation
//   http://0.0.0.0:8080/createbook → CreateBook operation
//
// Both WSDLs are served at ?wsdl:
//   http://localhost:8080/getbook?wsdl
//   http://localhost:8080/createbook?wsdl
// =============================================================================
public class ServerMain {

    public static void main(String[] args) throws Exception {

        // =====================================================================
        // The addresses to publish on.
        //
        // "0.0.0.0" means: listen on ALL network interfaces.
        // This is important inside Docker — if you use "localhost" or "127.0.0.1"
        // the service is only reachable from inside the same container.
        // With "0.0.0.0" other containers (and your host machine) can connect.
        //
        // Port 8080: the port this HTTP server listens on.
        // Path /getbook, /createbook: the URL paths of the SOAP endpoints.
        // =====================================================================
        String getBookAddress    = "http://0.0.0.0:8080/getbook";
        String createBookAddress = "http://0.0.0.0:8080/createbook";

        // =====================================================================
        // Endpoint.publish(address, implementor) — this single call does A LOT:
        //   1. Reads the SIB class and its @WebService annotations
        //   2. Locates the SEI via endpointInterface
        //   3. Loads the pre-written WSDL from the classpath (wsdlLocation)
        //   4. Starts the built-in HTTP server
        //   5. Registers a handler at /getbook (or /createbook) for SOAP POSTs
        //   6. Registers a handler at ?wsdl to serve the WSDL document
        //
        // From this point on, the server is live and accepting connections.
        // =====================================================================

        // Publish GetBook endpoint
        Endpoint getBookEndpoint = GetBookPortTypeImpl.publish(getBookAddress);
        System.out.println("GetBook   endpoint: " + getBookAddress);
        System.out.println("GetBook   WSDL    : " + getBookAddress + "?wsdl");

        // Publish CreateBook endpoint
        Endpoint createBookEndpoint = CreateBookPortTypeImpl.publish(createBookAddress);
        System.out.println("CreateBook endpoint: " + createBookAddress);
        System.out.println("CreateBook WSDL    : " + createBookAddress + "?wsdl");

        System.out.println("\nBook Service is running. Waiting for requests...\n");
        System.out.println("  Valid credentials: username=bookapp / password=secret123");

        // =====================================================================
        // Graceful shutdown hook.
        //
        // When the JVM receives SIGTERM (e.g. docker stop, Ctrl+C),
        // the JVM runs this thread before exiting.
        // endpoint.stop() cleanly shuts down the HTTP server for each endpoint.
        // =====================================================================
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            for (Endpoint ep : Arrays.asList(getBookEndpoint, createBookEndpoint)) {
                try { ep.stop(); } catch (Exception ignored) {}
            }
        }));

        // =====================================================================
        // Block the main thread forever.
        //
        // Endpoint.publish() starts the HTTP server on a BACKGROUND thread.
        // If main() returns, the JVM exits and takes the server with it.
        // Thread.currentThread().join() blocks main forever (until interrupted),
        // keeping the server alive.
        // =====================================================================
        Thread.currentThread().join();
    }
}
