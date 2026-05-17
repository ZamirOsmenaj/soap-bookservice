package com.bookservice.ws.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

// =============================================================================
// CreateBookOutput — the RESPONSE payload for the CreateBook operation
// =============================================================================
// Maps to <xsd:element name="CreateBookOutput"> in book.xsd.
// The server responds with a generated bookId, a status, and an optional message.
// =============================================================================
@XmlRootElement(name = "CreateBookOutput", namespace = "http://bookservice.com/book/v1")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name      = "",
    namespace = "http://bookservice.com/book/v1",
    propOrder = { "bookId", "status", "message" }
)
public class CreateBookOutput {

    @XmlElement(name = "bookId", namespace = "http://bookservice.com/book/v1", required = true)
    private String bookId;

    // =============================================================================
    // Status field maps to the XSD enumeration:
    //   <xsd:enumeration value="CREATED"/>
    //   <xsd:enumeration value="DUPLICATE"/>
    //
    // In wsimport-generated code this would be a Java enum class.
    // In our hand-written code we use String for simplicity, but in production
    // you'd define an enum: public enum BookStatus { CREATED, DUPLICATE }
    // =============================================================================
    @XmlElement(name = "status", namespace = "http://bookservice.com/book/v1", required = true)
    private String status;

    // required = false → minOccurs="0" in the XSD
    @XmlElement(name = "message", namespace = "http://bookservice.com/book/v1", required = false)
    private String message;

    public CreateBookOutput() {}

    public CreateBookOutput(String bookId, String status, String message) {
        this.bookId   = bookId;
        this.status   = status;
        this.message  = message;
    }

    public String getBookId()                      { return bookId; }
    public void   setBookId(String bookId)         { this.bookId = bookId; }

    public String getStatus()                      { return status; }
    public void   setStatus(String status)         { this.status = status; }

    public String getMessage()                     { return message; }
    public void   setMessage(String message)       { this.message = message; }
}
