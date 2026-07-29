package com.bookservice.springserver.interceptor;

import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.SoapBody;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.SoapMessage;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Locale;

// =============================================================================
// CredentialsInterceptor — Spring-WS equivalent of CredentialsHandler
// =============================================================================
// In the plain-Java version:
//   CredentialsHandler implements SOAPHandler<SOAPMessageContext>
//   Wired via: @HandlerChain(file="/handler-chain.xml") on each endpoint class
//
// In Spring-WS:
//   CredentialsInterceptor implements EndpointInterceptor
//   Wired via: WebServiceConfig.addInterceptors()
//
// The logic is identical:
//   - Intercept every inbound SOAP request
//   - Extract WsCredentials from the SOAP Header
//   - Validate username and password
//   - If invalid, write a SOAP Fault to the response and return false (abort)
//   - If valid, return true (proceed to the endpoint method)
//
// NOTE on javax.xml.soap:
//   We deliberately avoid importing javax.xml.soap.* here.
//   That package (SAAJ) was removed from the JDK in Java 11 and must be added
//   as an explicit Maven dependency. Instead we use Spring-WS's own abstraction
//   layer (SoapBody, SoapFault, etc.) which is always available via spring-ws-core
//   and works identically regardless of the underlying SAAJ implementation.
// =============================================================================
public class CredentialsInterceptor implements EndpointInterceptor {

    private static final String CMN_NS   = "http://bookservice.com/common/v1";
    private static final String CRED_EL  = "WsCredentials";
    private static final String USER_EL  = "username";
    private static final String PASS_EL  = "password";

    // Hardcoded credentials — same as the plain-Java version (demo only)
    private static final String VALID_USERNAME = "bookapp";
    private static final String VALID_PASSWORD = "secret123";

    // =========================================================================
    // handleRequest — called before the endpoint method runs
    // =========================================================================
    // Returns:
    //   true  → continue to next interceptor or to the endpoint method
    //   false → abort; Spring-WS sends the response as-is (fault already written)
    // =========================================================================
    @Override
    public boolean handleRequest(MessageContext messageContext, Object endpoint) throws Exception {

        SoapMessage soapRequest = (SoapMessage) messageContext.getRequest();
        SoapHeader  soapHeader  = soapRequest.getSoapHeader();

        if (soapHeader == null) {
            System.err.println("[CredentialsInterceptor] REJECTED: No SOAP Header present.");
            writeFault(messageContext, "Missing SOAP Header: WsCredentials is required.");
            return false;
        }

        // Iterate header elements looking for WsCredentials
        Iterator<SoapHeaderElement> it = soapHeader.examineAllHeaderElements();
        while (it.hasNext()) {
            SoapHeaderElement element = it.next();
            QName name = element.getName();

            if (CMN_NS.equals(name.getNamespaceURI()) && CRED_EL.equals(name.getLocalPart())) {
                // Found WsCredentials — extract username/password via the DOM
                Element domElement = toDomElement(element);
                String username = getChildText(domElement, USER_EL);
                String password = getChildText(domElement, PASS_EL);

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    System.out.println("[CredentialsInterceptor] ACCEPTED: user=" + username);
                    return true;
                } else {
                    System.err.println("[CredentialsInterceptor] REJECTED: bad credentials for user=" + username);
                    writeFault(messageContext, "Invalid credentials. Access denied.");
                    return false;
                }
            }
        }

        System.err.println("[CredentialsInterceptor] REJECTED: WsCredentials element not found in Header.");
        writeFault(messageContext, "Missing WsCredentials in SOAP Header.");
        return false;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext, Object endpoint) {
        return true; // pass through
    }

    @Override
    public boolean handleFault(MessageContext messageContext, Object endpoint) {
        return true; // pass through
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) {
        // nothing to clean up
    }

    // =========================================================================
    // writeFault — writes a SOAP Fault using Spring-WS's SoapBody abstraction
    // =========================================================================
    // Spring-WS SoapBody.addClientOrSenderFault() handles everything:
    //   - faultcode  = "Client"  (SOAP 1.1) or "Sender" (SOAP 1.2)
    //   - faultstring = the message we provide
    //   - locale     = Locale.ENGLISH for the faultstring xml:lang attribute
    //
    // This approach requires zero javax.xml.soap imports — it works entirely
    // through the Spring-WS abstraction layer, which delegates to the underlying
    // SAAJ or Axiom implementation transparently.
    // =========================================================================
    private void writeFault(MessageContext messageContext, String faultString) {
        try {
            SoapMessage  soapResponse = (SoapMessage) messageContext.getResponse();
            SoapBody     soapBody     = soapResponse.getSoapBody();
            soapBody.addClientOrSenderFault(faultString, Locale.ENGLISH);
        } catch (Exception e) {
            System.err.println("[CredentialsInterceptor] Failed to write fault: " + e.getMessage());
        }
    }

    // =========================================================================
    // toDomElement — transforms a SoapHeaderElement's Source into a DOM Element
    // =========================================================================
    // Spring-WS exposes every header element as a javax.xml.transform.Source.
    // We transform it into a DOMResult so we can navigate children with the DOM API.
    // javax.xml.transform is part of the JDK and always available — no extra dep.
    // =========================================================================
    private Element toDomElement(SoapHeaderElement headerElement) {
        try {
            javax.xml.transform.Source source = headerElement.getSource();
            javax.xml.transform.dom.DOMResult result = new javax.xml.transform.dom.DOMResult();
            javax.xml.transform.TransformerFactory.newInstance()
                    .newTransformer()
                    .transform(source, result);
            org.w3c.dom.Node node = result.getNode();
            if (node instanceof org.w3c.dom.Document) {
                return ((org.w3c.dom.Document) node).getDocumentElement();
            }
            if (node instanceof Element) {
                return (Element) node;
            }
        } catch (Exception e) {
            System.err.println("[CredentialsInterceptor] Failed to parse header element: " + e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // getChildText — reads the text of a named child element (namespace-aware)
    // =========================================================================
    private String getChildText(Element parent, String localName) {
        if (parent == null) return null;
        // Try namespace-qualified first (elementFormDefault="qualified" in common.xsd)
        NodeList nodes = parent.getElementsByTagNameNS(CMN_NS, localName);
        if (nodes.getLength() == 0) {
            // Fallback: some parsers strip namespace from child elements
            nodes = parent.getElementsByTagName(localName);
        }
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }
}
