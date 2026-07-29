package com.bookservice.springserver.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.SimpleWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import com.bookservice.springserver.interceptor.CredentialsInterceptor;

import java.util.List;

// =============================================================================
// WebServiceConfig — Spring-WS configuration
// =============================================================================
// This class is the Spring Boot equivalent of:
//   1. ServerMain.Endpoint.publish(address, impl)   → MessageDispatcherServlet
//   2. handler-chain.xml + @HandlerChain            → EndpointInterceptor
//   3. wsdlLocation in @WebService                  → SimpleWsdl11Definition beans
//
// HOW SPRING-WS ROUTING WORKS:
//   Every SOAP request hits the MessageDispatcherServlet (registered at "/ws/*").
//   The servlet uses the SOAP action or the root element's local name to decide
//   which @Endpoint bean and which @PayloadRoot method to dispatch to.
//
//   @PayloadRoot(namespace = "...", localPart = "GetBookInput")
//   → "when the SOAP Body root element is <bk:GetBookInput>, call this method"
//
// This is different from the plain-Java approach where each operation had its
// own dedicated URL path (/getbook, /createbook). In Spring-WS, all operations
// share ONE servlet path (/ws/*) and are distinguished by their payload root.
// =============================================================================
@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    // =========================================================================
    // MessageDispatcherServlet
    // =========================================================================
    // This is the SOAP front controller — the single entry point for all
    // incoming SOAP messages. It is the Spring-WS equivalent of a DispatcherServlet.
    //
    // We register it at "/ws/*" so:
    //   SOAP endpoint for GetBook:    POST http://host:8080/ws/getbook
    //   SOAP endpoint for CreateBook: POST http://host:8080/ws/createbook
    //   WSDL for GetBook:             GET  http://host:8080/ws/getbook.wsdl
    //   WSDL for CreateBook:          GET  http://host:8080/ws/createbook.wsdl
    //
    // transformWsdlLocations = true:
    //   Spring-WS rewrites the <soap:address location="..."> in the served WSDL
    //   to match the actual request URL at runtime. This means the WSDL always
    //   shows the correct server address regardless of environment (dev/prod/Docker).
    //   In the plain-Java version the WSDL had "REPLACE_WITH_ACTUAL_URL" as a
    //   placeholder — Spring-WS eliminates that manual step entirely.
    // =========================================================================
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {

        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    // =========================================================================
    // WSDL definitions
    // =========================================================================
    // Each SimpleWsdl11Definition bean serves one WSDL file.
    // The bean NAME determines the URL path:
    //   Bean name "getbook"    → served at GET /ws/getbook.wsdl
    //   Bean name "createbook" → served at GET /ws/createbook.wsdl
    //
    // Spring-WS reads the file from the classpath (src/main/resources/wsdl/)
    // and serves it at runtime, patching the <soap:address location> with the
    // real URL (because transformWsdlLocations=true above).
    //
    // In the plain-Java version, wsdlLocation="wsdl/GetBook.wsdl" in @WebService
    // did this job. Here, these @Bean methods replace that annotation attribute.
    // =========================================================================
    @Bean(name = "getbook")
    public SimpleWsdl11Definition getBookWsdl() {
        SimpleWsdl11Definition definition = new SimpleWsdl11Definition();
        definition.setWsdl(new ClassPathResource("wsdl/GetBook.wsdl"));
        return definition;
    }

    @Bean(name = "createbook")
    public SimpleWsdl11Definition createBookWsdl() {
        SimpleWsdl11Definition definition = new SimpleWsdl11Definition();
        definition.setWsdl(new ClassPathResource("wsdl/CreateBook.wsdl"));
        return definition;
    }

    // =========================================================================
    // XSD schema beans — optional but good practice.
    // Serving the XSD files lets clients and tools resolve imports from the WSDL.
    // Bean name = URL path segment: /ws/book.xsd, /ws/common.xsd
    // =========================================================================
    @Bean(name = "book")
    public XsdSchema bookSchema() {
        return new SimpleXsdSchema(new ClassPathResource("wsdl/book.xsd"));
    }

    @Bean(name = "common")
    public XsdSchema commonSchema() {
        return new SimpleXsdSchema(new ClassPathResource("wsdl/common.xsd"));
    }

    // =========================================================================
    // Interceptors — replaces handler-chain.xml + @HandlerChain
    // =========================================================================
    // In the plain-Java version, CredentialsHandler was wired in via:
    //   @HandlerChain(file = "/handler-chain.xml") on the implementation class.
    //
    // In Spring-WS, interceptors are registered here and apply globally to ALL
    // @Endpoint beans — the same cross-cutting behaviour, but configured in one
    // central place rather than annotating each endpoint class.
    //
    // Interceptor execution order mirrors the plain-Java handler chain:
    //   Inbound:  interceptor.handleRequest() → endpoint method
    //   Outbound: endpoint method → interceptor.handleResponse()
    // =========================================================================
    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(new CredentialsInterceptor());
    }
}
