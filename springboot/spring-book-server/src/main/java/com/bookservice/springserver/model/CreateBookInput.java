package com.bookservice.springserver.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

// Maps to <xsd:element name="CreateBookInput"> in book.xsd
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
    public CreateBookInput(BookType book) { this.book = book; }

    public BookType getBook()              { return book; }
    public void     setBook(BookType book) { this.book = book; }
}
