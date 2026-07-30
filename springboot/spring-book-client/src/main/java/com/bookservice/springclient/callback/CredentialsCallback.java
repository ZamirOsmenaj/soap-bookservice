package com.bookservice.springclient.callback;

import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPHeaderElement;
import jakarta.xml.soap.SOAPMessage;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;

// =============================================================================
// CredentialsCallback — injects WsCredentials into the SOAP Header
// =============================================================================
// In the plain-Java client, a CredentialsInjector SOAPHandler was added to
// the port binding's handler chain and ran automatically on every outbound call.
//
// In Spring-WS, WebServiceMessageCallback is the equivalent:
//   - Passed as the third argument to webServiceTemplate.marshalSendAndReceive()
//   - Called AFTER the SOAP Body has been marshalled but BEFORE the HTTP send
//   - Gives us access to the full SoapMessage so we can add a SOAP Header
//
// We create a new CredentialsCallback instance per call in BookServiceClient,
// which lets us vary the credentials per scenario (valid / invalid demo).
//
// NOTE on jakarta.xml.soap vs javax.xml.soap:
//   saaj-impl 3.x (Jakarta EE SAAJ) ships under the jakarta.xml.soap namespace.
//   The old javax.xml.soap namespace was removed from the JDK after Java 8.
//   All SAAJ imports here use jakarta.xml.soap.* accordingly.
//
// Wire format produced:
//   <soapenv:Header>
//     <cmn:WsCredentials xmlns:cmn="http://bookservice.com/common/v1">
//       <cmn:username>bookapp</cmn:username>
//       <cmn:password>secret123</cmn:password>
//       <cmn:systemId>SPRING_BOOK_CLIENT</cmn:systemId>
//     </cmn:WsCredentials>
//   </soapenv:Header>
// =============================================================================
public class CredentialsCallback implements WebServiceMessageCallback {

    private static final String CMN_NS = "http://bookservice.com/common/v1";

    private final String username;
    private final String password;
    private final String systemId;

    public CredentialsCallback(String username, String password, String systemId) {
        this.username = username;
        this.password = password;
        this.systemId = systemId;
    }

    // =========================================================================
    // doWithMessage — called by WebServiceTemplate before sending the request
    // =========================================================================
    // We cast to SaajSoapMessage to get the raw SAAJ SOAPMessage, then use
    // the SAAJ API (jakarta.xml.soap.*) to build the WsCredentials header element.
    // This is the same approach as the plain-Java CredentialsInjector, just with
    // jakarta.* imports instead of javax.*.
    // =========================================================================
    @Override
    public void doWithMessage(WebServiceMessage message) throws TransformerException {
        try {
            SaajSoapMessage saajMessage = (SaajSoapMessage) message;
            SOAPMessage     rawMessage  = saajMessage.getSaajMessage();
            SOAPEnvelope    envelope    = rawMessage.getSOAPPart().getEnvelope();

            // Ensure the SOAP Header block exists
            SOAPHeader header = envelope.getHeader();
            if (header == null) {
                header = envelope.addHeader();
            }

            // Add <cmn:WsCredentials> to the header
            SOAPHeaderElement credentials = header.addHeaderElement(
                    envelope.createName("WsCredentials", "cmn", CMN_NS));

            // Add child elements with the same namespace
            credentials.addChildElement(envelope.createName("username", "cmn", CMN_NS))
                       .addTextNode(username);

            credentials.addChildElement(envelope.createName("password", "cmn", CMN_NS))
                       .addTextNode(password);

            credentials.addChildElement(envelope.createName("systemId", "cmn", CMN_NS))
                       .addTextNode(systemId);

            rawMessage.saveChanges();
        } catch (Exception e) {
            throw new TransformerException("Failed to inject WsCredentials header: " + e.getMessage(), e);
        }
    }
}
