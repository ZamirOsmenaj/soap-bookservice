package com.bookservice.springclient.config;

import com.bookservice.springclient.model.BookType;
import com.bookservice.springclient.model.CreateBookInput;
import com.bookservice.springclient.model.CreateBookOutput;
import com.bookservice.springclient.model.GetBookInput;
import com.bookservice.springclient.model.GetBookOutput;
import com.bookservice.springclient.model.WsFault;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

// =============================================================================
// WebServiceClientConfig — wires the WebServiceTemplate
// =============================================================================
// In the plain-Java client, the SOAP infrastructure was set up manually:
//   1. new GetBookService(new URL(wsdlUrl))     ← factory from wsimport stubs
//   2. factory.getGetBookPort()                 ← proxy object
//   3. ((BindingProvider) proxy).getRequestContext().put(...)  ← override URL
//   4. binding.getHandlerChain().add(new CredentialsInjector()) ← add handler
//
// In Spring-WS, WebServiceTemplate replaces all of that:
//   - marshalSendAndReceive(uri, request) marshals the request object to XML,
//     sends it as a SOAP Body, receives the response, and unmarshals it back
//     to a Java object — all in one call.
//   - The Jaxb2Marshaller tells Spring-WS which JAXB classes to use for
//     marshal/unmarshal. It must know all classes that can appear in the Body.
//   - No wsimport, no generated stubs, no factory — just a template + marshaller.
// =============================================================================
@Configuration
public class WebServiceClientConfig {

    @Value("${server.host:spring-book-server}")
    private String serverHost;

    // =========================================================================
    // Jaxb2Marshaller
    // =========================================================================
    // Registers all JAXB-annotated classes that can appear as SOAP Body content.
    // Spring-WS uses this to:
    //   - Marshal Java objects → XML (for outgoing requests)
    //   - Unmarshal XML → Java objects (for incoming responses)
    //
    // classesToBeBound replaces the package scan because we have classes in
    // two namespaces (book/v1 and common/v1). Listing them explicitly is safer
    // and avoids accidentally picking up unrelated classes.
    // =========================================================================
    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(
            GetBookInput.class,
            GetBookOutput.class,
            CreateBookInput.class,
            CreateBookOutput.class,
            BookType.class,
            WsFault.class
        );
        return marshaller;
    }

    // =========================================================================
    // WebServiceTemplate
    // =========================================================================
    // The central class for sending and receiving SOAP messages.
    // We set the same marshaller for both marshalling and unmarshalling.
    //
    // defaultUri is the base URL — callers can pass a full URI per call,
    // but setting a default here keeps the demo clean.
    // =========================================================================
    @Bean
    public WebServiceTemplate webServiceTemplate(Jaxb2Marshaller marshaller) {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        template.setDefaultUri("http://" + serverHost + ":8080/ws");
        return template;
    }
}
