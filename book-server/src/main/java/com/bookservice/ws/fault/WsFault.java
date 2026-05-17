package com.bookservice.ws.fault;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

// =============================================================================
// WHAT IS THIS CLASS?
// =============================================================================
// This is the Java representation of the <cmn:WsFault> defined in common.xsd.
//
// When the server throws a WsException (below), JAX-WS serializes this object
// into XML and places it inside the <detail> block of the SOAP Fault:
//
//   <soapenv:Fault>
//     <faultcode>soapenv:Server</faultcode>
//     <faultstring>Business error occurred</faultstring>
//     <detail>
//       <cmn:WsFault xmlns:cmn="http://bookservice.com/common/v1">
//         <cmn:errorCode>BOOK_NOT_FOUND</cmn:errorCode>
//         <cmn:errorMessage>No book with ISBN 978-x</cmn:errorMessage>
//         <cmn:errorTimestamp>2024-01-15T10:30:00</cmn:errorTimestamp>
//         <cmn:operationName>GetBook</cmn:operationName>
//       </cmn:WsFault>
//     </detail>
//   </soapenv:Fault>
//
// WHY do we write this class manually and not generate it?
// The server's SEI (GetBookPortType, CreateBookPortType) is hand-written,
// so its associated fault class is also hand-written. This gives us full
// control over the fault structure and Java type mapping.
// =============================================================================

// =============================================================================
// JAXB Annotations
// =============================================================================
// JAXB (Java Architecture for XML Binding) is the technology that converts
// Java objects ↔ XML. JAX-WS uses JAXB under the hood for all marshalling.

// @XmlAccessorType(XmlAccessType.FIELD)
//   → JAXB should use FIELDS (not getters/setters) to determine what to marshal.
//     This means the @XmlElement annotations on the fields below are what matter.
//     Without this, JAXB defaults to XmlAccessType.PUBLIC_MEMBER (uses getters).

// @XmlType
//   → Marks this class as an XSD complexType.
//   name = "" → matches the name in common.xsd: <xsd:complexType name="">
//   namespace = "http://bookservice.com/common/v1" → matches targetNamespace in common.xsd
//   propOrder = {...} → CRITICAL: the fields must be listed in the EXACT ORDER
//                       they appear in the XSD <xsd:sequence>. JAXB respects this
//                       order when serializing. If the order differs from the XSD,
//                       XML validation will fail.
// =============================================================================
@XmlRootElement(name = "WsFault", namespace = "http://bookservice.com/common/v1")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "",
        namespace = "http://bookservice.com/common/v1",
        propOrder = {"errorCode", "errorMessage", "errorTimestamp", "operationName"}
)
public class WsFault {

    // =========================================================================
    // @XmlElement
    // =========================================================================
    // Maps this Java field to an XML element.
    //
    // name = "errorCode"
    //   → The XML element will be named <cmn:errorCode>
    //     Matches the <xsd:element name="errorCode"> in common.xsd
    //
    // namespace = "http://bookservice.com/common/v1"
    //   → Because elementFormDefault="qualified" in common.xsd, ALL child
    //     elements must carry the namespace. This is why every @XmlElement
    //     has the namespace attribute.
    //
    // required = true
    //   → This field is mandatory (no minOccurs="0" in the XSD).
    //     JAXB will throw a validation error if this is null during marshalling.
    // =========================================================================
    @XmlElement(name = "errorCode", namespace = "http://bookservice.com/common/v1", required = true)
    private String errorCode;

    @XmlElement(name = "errorMessage", namespace = "http://bookservice.com/common/v1", required = true)
    private String errorMessage;

    // =========================================================================
    // XMLGregorianCalendar
    // =========================================================================
    // The Java type for xsd:dateTime is javax.xml.datatype.XMLGregorianCalendar.
    // It handles the full xsd:dateTime format: 2024-01-15T10:30:00.000Z
    //
    // To create one: DatatypeFactory.newInstance().newXMLGregorianCalendar(...)
    // Or from a GregorianCalendar: DatatypeFactory.newInstance().newXMLGregorianCalendar(gc)
    //
    // This is verbose — in practice teams often write a utility method.
    // =========================================================================
    @XmlElement(name = "errorTimestamp", namespace = "http://bookservice.com/common/v1", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar errorTimestamp;

    // nillable = true → allows <cmn:operationName xsi:nil="true"/> in XML (optional field)
    // required = false (default) → matches minOccurs="0" in the XSD
    @XmlElement(name = "operationName", namespace = "http://bookservice.com/common/v1", required = false)
    private String operationName;

    // Standard no-arg constructor required by JAXB
    public WsFault() {
    }

    public WsFault(String errorCode, String errorMessage,
                       XMLGregorianCalendar errorTimestamp, String operationName) {
        this.errorCode      = errorCode;
        this.errorMessage   = errorMessage;
        this.errorTimestamp = errorTimestamp;
        this.operationName  = operationName;
    }

    public String getErrorCode() {
        return this.errorCode;
    }
    public void setErrorCode(String value) {
        this.errorCode = value;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
    public void setErrorMessage(String value) {
        this.errorMessage = value;
    }

    public XMLGregorianCalendar getErrorTimestamp() {
        return this.errorTimestamp;
    }
    public void setErrorTimestamp(XMLGregorianCalendar value) {
        this.errorTimestamp = value;
    }

    public String getOperationName() {
        return this.operationName;
    }
    public void setOperationName(String value) {
        this.operationName = value;
    }
}
