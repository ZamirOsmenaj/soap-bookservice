package com.bookservice.ws.handler;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

// =============================================================================
// WHAT IS A JAX-WS HANDLER?
// =============================================================================
// A Handler is a class that intercepts SOAP messages BEFORE they reach
// the service implementation and AFTER responses leave it.
//
// Think of it as a middleware/filter:
//
//   Client Request → [Handler chain] → Service Implementation → [Handler chain] → Client Response
//
// Handlers are used for cross-cutting concerns:
//   • Authentication / Authorization (what we do here)
//   • Logging / Auditing
//   • Message transformation / enrichment
//   • Compression, encryption
//
// There are two types:
//   SOAPHandler  → has access to the full SOAP XML (Header + Body)
//   LogicalHandler → has access only to the payload (Body content), not the envelope
//
// We use SOAPHandler because we need to read the SOAP Header.
// =============================================================================
public class CredentialsHandler implements SOAPHandler<SOAPMessageContext> {

    // Namespace constants — these must match common.xsd and the SEI
    private static final String CMN_NS   = "http://bookservice.com/common/v1";
    private static final String CRED_EL  = "WsCredentials";
    private static final String USER_EL  = "username";
    private static final String PASS_EL  = "password";

    // Hardcoded credentials for demo purposes.
    // In production: check against a database, LDAP, or secrets manager.
    private static final String VALID_USERNAME = "bookapp";
    private static final String VALID_PASSWORD = "secret123";

    // =============================================================================
    // getHeaders()
    // =============================================================================
    // Returns the set of SOAP header QNames that this handler UNDERSTANDS.
    // This is used by the JAX-WS runtime to decide whether a "mustUnderstand"
    // header has been processed (preventing a fault for unprocessed headers).
    //
    // QName = Qualified Name = {namespace}localPart
    // We return {http://bookservice.com/common/v1}WsCredentials
    // telling the runtime: "I know how to handle the WsCredentials header."
    // =============================================================================
    @Override
    public Set<QName> getHeaders() {
        return Collections.singleton(new QName(CMN_NS, CRED_EL));
    }

    // =============================================================================
    // handleMessage()
    // =============================================================================
    // Called for EVERY message (both inbound requests and outbound responses).
    //
    // MessageContext.MESSAGE_OUTBOUND_PROPERTY:
    //   → true  = outbound (response leaving the server)
    //   → false = inbound  (request arriving at the server)
    //
    // We only validate credentials on INBOUND messages (when a client is calling us).
    // For outbound messages we just pass through.
    //
    // Return value:
    //   true  = continue processing (pass to next handler or to the service impl)
    //   false = stop processing (abort the handler chain, don't call the service)
    // =============================================================================
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean isOutbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (Boolean.TRUE.equals(isOutbound)) {
            // Outbound response — nothing to validate, let it pass through
            return true;
        }

        // It is an inbound request — validate credentials from the SOAP header
        try {
            SOAPEnvelope envelope = context.getMessage().getSOAPPart().getEnvelope();
            SOAPHeader   header   = envelope.getHeader();

            if (header == null) {
                System.err.println("[CredentialsHandler] REJECTED: No SOAP Header present.");
                throwAuthFault(context, "Missing SOAP Header: WsCredentials is required.");
                return false;
            }

            // =====================================================================
            // Navigate the SOAP Header XML to find WsCredentials
            // =====================================================================
            // The header looks like:
            //   <soapenv:Header>
            //     <cmn:WsCredentials xmlns:cmn="http://bookservice.com/common/v1">
            //       <cmn:username>bookapp</cmn:username>
            //       <cmn:password>secret123</cmn:password>
            //       <cmn:systemId>WEBAPP</cmn:systemId>
            //     </cmn:WsCredentials>
            //   </soapenv:Header>
            // =====================================================================
            Iterator<?> credIterator = header.getChildElements(
                    new QName(CMN_NS, CRED_EL));

            if (!credIterator.hasNext()) {
                System.err.println("[CredentialsHandler] REJECTED: WsCredentials element not found in header.");
                throwAuthFault(context, "Missing WsCredentials in SOAP Header.");
                return false;
            }

            SOAPHeaderElement credentialsEl = (SOAPHeaderElement) credIterator.next();

            // Extract username child element
            String username = getChildText(credentialsEl, USER_EL);
            String password = getChildText(credentialsEl, PASS_EL);

            if (username == null || password == null) {
                System.err.println("[CredentialsHandler] REJECTED: Missing username or password.");
                throwAuthFault(context, "WsCredentials must contain username and password.");
                return false;
            }

            // =====================================================================
            // Validate credentials
            // In production: call an AuthService, UserRepository, or OAuth provider.
            // Never compare passwords in plain text — use BCrypt or similar.
            // =====================================================================
            if (!VALID_USERNAME.equals(username) || !VALID_PASSWORD.equals(password)) {
                System.err.println("[CredentialsHandler] REJECTED: Invalid credentials for user: " + username);
                throwAuthFault(context, "Invalid credentials. Access denied.");
                return false;
            }

            System.out.println("[CredentialsHandler] ACCEPTED: Authenticated user: " + username);
            return true; // ← proceed to the service implementation

        } catch (Exception e) {
            System.err.println("[CredentialsHandler] ERROR: " + e.getMessage());
            throwAuthFault(context, "Authentication error: " + e.getMessage());
            return false;
        }
    }

    // =============================================================================
    // handleFault()
    // =============================================================================
    // Called when a SOAP Fault is being processed through the handler chain.
    // We just log it and pass it through.
    // =============================================================================
    @Override
    public boolean handleFault(SOAPMessageContext context) {
        System.err.println("[CredentialsHandler] Fault message passing through handler.");
        return true;
    }

    @Override
    public void close(MessageContext context) {
        // Nothing to clean up
    }

    // =============================================================================
    // Helper: extract text content of a named child element
    // =============================================================================
    private String getChildText(SOAPElement parent, String localName) {
        Iterator<?> it = parent.getChildElements(new QName(CMN_NS, localName));
        if (it.hasNext()) {
            return ((SOAPElement) it.next()).getValue();
        }
        return null;
    }

    // =============================================================================
    // Helper: set a SOAP Fault on the response message
    // =============================================================================
    // When authentication fails we modify the OUTBOUND message to be a SOAP Fault.
    // The SOAP Fault format:
    //   <soapenv:Fault>
    //     <faultcode>soapenv:Client</faultcode>   ← "Client" = caller's fault
    //     <faultstring>Invalid credentials...</faultstring>
    //   </soapenv:Fault>
    //
    // faultcode values:
    //   soapenv:Client → the request was bad (auth failure = client's fault)
    //   soapenv:Server → something went wrong on the server side
    // =============================================================================
    private void throwAuthFault(SOAPMessageContext context, String message) {
        try {
            SOAPEnvelope envelope = context.getMessage().getSOAPPart().getEnvelope();
            SOAPBody body = envelope.getBody();
            body.addFault().setFaultString(message);
            body.getFault().setFaultCode("soapenv:Client");
        } catch (Exception ex) {
            System.err.println("[CredentialsHandler] Could not set fault on message: " + ex.getMessage());
        }
    }
}
