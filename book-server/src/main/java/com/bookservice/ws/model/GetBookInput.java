package com.bookservice.ws.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

// =============================================================================
// GetBookInput — the REQUEST payload for the GetBook operation
// =============================================================================
// Maps to <xsd:element name="GetBookInput"> in book.xsd.
//
// @XmlRootElement vs @XmlType:
//   @XmlType     → defines an XSD complexType (a reusable type definition)
//   @XmlRootElement → defines an XSD element (a standalone XML element)
//
// We use @XmlRootElement here because GetBookInput IS a top-level element
// in the XSD (<xsd:element name="GetBookInput">), not just a type.
// It appears directly as the child of <soapenv:Body>.
//
// name = "GetBookInput"       → the XML element name
// namespace = "..."           → matches targetNamespace in book.xsd
// =============================================================================
@XmlRootElement(name = "GetBookInput", namespace = "http://bookservice.com/book/v1")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name      = "",
    namespace = "http://bookservice.com/book/v1",
    propOrder = { "isbn" }
)
public class GetBookInput {

    // The only input needed: the ISBN of the book to look up.
    @XmlElement(name = "isbn", namespace = "http://bookservice.com/book/v1", required = true)
    private String isbn;

    public GetBookInput() {}

    public GetBookInput(String isbn) {
        this.isbn = isbn;
    }

    public String getIsbn()                { return isbn; }
    public void   setIsbn(String isbn)     { this.isbn = isbn; }
}
