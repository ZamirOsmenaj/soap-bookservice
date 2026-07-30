package com.bookservice.springclient;

import com.bookservice.springclient.client.BookServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// =============================================================================
// SpringBookClientApplication — Spring Boot entry point for the SOAP client
// =============================================================================
// In the plain-Java version, BookClient had a main() that:
//   1. Called waitForServer() in a loop
//   2. Ran the demo scenarios
//   3. Exited
//
// Here, CommandLineRunner.run() is the equivalent of main() logic:
//   - Spring Boot calls run() after the context is fully initialised
//   - All @Autowired beans (BookServiceClient, WebServiceTemplate, etc.)
//     are ready before run() is invoked
//   - When run() returns, Spring Boot exits the JVM (because
//     spring.main.web-application-type=none in application.properties)
//
// The server wait loop is handled in run() below using the same
// poll-until-ready approach as the plain-Java version.
// =============================================================================
@SpringBootApplication
public class SpringBookClientApplication implements CommandLineRunner {

    @Autowired
    private BookServiceClient bookServiceClient;

    public static void main(String[] args) {
        SpringApplication.run(SpringBookClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Give the server a moment if it just started, then run the demo
        bookServiceClient.runDemo();
    }
}
