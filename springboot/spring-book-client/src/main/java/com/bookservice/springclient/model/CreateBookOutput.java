package com.bookservice.springclient.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "CreateBookOutput", namespace = "http://bookservice.com/book/v1")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name      = "",
    namespace = "http://bookservice.com/book/v1",
    propOrder = { "bookId", "status", "message" }
)
public class CreateBookOutput {

    @XmlElement(name = "bookId",  namespace = "http://bookservice.com/book/v1", required = true)
    private String bookId;

    @XmlElement(name = "status",  namespace = "http://bookservice.com/book/v1", required = true)
    private String status;

    @XmlElement(name = "message", namespace = "http://bookservice.com/book/v1", required = false)
    private String message;

    public CreateBookOutput() {}

    public CreateBookOutput(String bookId, String status, String message) {
        this.bookId  = bookId;
        this.status  = status;
        this.message = message;
    }

    public String getBookId()                { return bookId; }
    public void   setBookId(String bookId)   { this.bookId = bookId; }
    public String getStatus()                { return status; }
    public void   setStatus(String status)   { this.status = status; }
    public String getMessage()               { return message; }
    public void   setMessage(String message) { this.message = message; }
}
