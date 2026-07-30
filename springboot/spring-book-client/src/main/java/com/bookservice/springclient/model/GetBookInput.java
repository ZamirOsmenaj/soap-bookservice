package com.bookservice.springclient.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "GetBookInput", namespace = "http://bookservice.com/book/v1")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name      = "",
    namespace = "http://bookservice.com/book/v1",
    propOrder = { "isbn" }
)
public class GetBookInput {

    @XmlElement(name = "isbn", namespace = "http://bookservice.com/book/v1", required = true)
    private String isbn;

    public GetBookInput() {}
    public GetBookInput(String isbn) { this.isbn = isbn; }

    public String getIsbn()            { return isbn; }
    public void   setIsbn(String isbn) { this.isbn = isbn; }
}
