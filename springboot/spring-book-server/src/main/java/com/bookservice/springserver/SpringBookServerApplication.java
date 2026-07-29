package com.bookservice.springserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// =============================================================================
// SpringBookServerApplication — Spring Boot entry point
// =============================================================================
// This replaces ServerMain from the plain-Java version.
//
// In the plain-Java version, ServerMain:
//   1. Called Endpoint.publish() for each operation (starts a raw HTTP server)
//   2. Blocked the main thread with Thread.currentThread().join()
//
// Here, SpringApplication.run() does all of that and much more:
//   1. Starts an embedded Tomcat server (on port 8080 from application.properties)
//   2. Discovers and registers all @Endpoint beans automatically
//   3. Registers the MessageDispatcherServlet (the SOAP front controller)
//   4. Wires in all interceptors, marshallers, and WSDL definitions
//   5. Manages lifecycle, health, and graceful shutdown automatically
//
// The @SpringBootApplication annotation enables:
//   @Configuration       → this class can define @Bean methods
//   @EnableAutoConfiguration → Spring Boot auto-configures what it finds on the classpath
//   @ComponentScan       → scans this package and sub-packages for @Component, @Endpoint, etc.
// =============================================================================
@SpringBootApplication
public class SpringBookServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBookServerApplication.class, args);
    }
}
