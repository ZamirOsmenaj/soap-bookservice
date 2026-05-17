package com.bookservice.ws.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

// =============================================================================
// GetBookOutput — the RESPONSE payload for the GetBook operation
// =============================================================================
// Maps to <xsd:element name="GetBookOutput"> in book.xsd.
// The server populates this and returns it via the SEI method's return value.
// JAX-WS marshals it into the SOAP Body as:
//
//   <bk:GetBookOutput>
//     <bk:book>
//       <cmn:isbn>978-0-13-468599-1</cmn:isbn>
//       <cmn:title>Effective Java</cmn:title>
//       ...
//     </bk:book>
//   </bk:GetBookOutput>
//
// NOTE on the "book" element's namespace:
//   The <bk:book> element itself belongs to the book/v1 namespace.
//   Its CHILDREN (isbn, title, etc.) also belong to book/v1 namespace.
//   This is handled automatically by BookType's @XmlElement namespace attributes.
// =============================================================================
@XmlRootElement(name = "GetBookOutput", namespace = "http://bookservice.com/book/v1")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name      = "",
    namespace = "http://bookservice.com/book/v1",
    propOrder = { "book" }
)
public class GetBookOutput {

    @XmlElement(name = "book", namespace = "http://bookservice.com/book/v1", required = true)
    private BookType book;

    public GetBookOutput() {}

    public GetBookOutput(BookType book) {
        this.book = book;
    }

    public BookType getBook()                  { return book; }
    public void     setBook(BookType book)     { this.book = book; }
}
