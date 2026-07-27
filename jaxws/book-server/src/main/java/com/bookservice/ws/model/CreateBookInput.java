package com.bookservice.ws.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

// =============================================================================
// CreateBookInput — the REQUEST payload for the CreateBook operation
// =============================================================================
// Maps to <xsd:element name="CreateBookInput"> in book.xsd.
// The client populates a BookType and sends it to create a new catalog entry.
// =============================================================================
@XmlRootElement(name = "CreateBookInput", namespace = "http://bookservice.com/book/v1")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name      = "",
    namespace = "http://bookservice.com/book/v1",
    propOrder = { "book" }
)
public class CreateBookInput {

    @XmlElement(name = "book", namespace = "http://bookservice.com/book/v1", required = true)
    private BookType book;

    public CreateBookInput() {}

    public CreateBookInput(BookType book) {
        this.book = book;
    }

    public BookType getBook()                  { return book; }
    public void     setBook(BookType book)     { this.book = book; }
}
