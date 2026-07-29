package com.bookservice.springserver.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.math.BigDecimal;

// =============================================================================
// BookType — core domain object
// =============================================================================
// Identical to the plain-Java version except for one thing:
//   javax.xml.bind.* → jakarta.xml.bind.*
//
// Java 17 removed the javax.xml.bind packages from the JDK (they were
// deprecated in Java 9 and removed in Java 11). The Jakarta EE project
// took over these APIs under the "jakarta.*" namespace. Spring Boot 3.x
// requires Java 17+ and uses Jakarta EE 10, so all JAXB imports here
// use "jakarta.xml.bind" instead of "javax.xml.bind".
// =============================================================================
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name      = "BookType",
    namespace = "http://bookservice.com/book/v1",
    propOrder = { "isbn", "title", "author", "price", "stockQuantity", "genre" }
)
public class BookType {

    @XmlElement(name = "isbn", namespace = "http://bookservice.com/book/v1", required = true)
    private String isbn;

    @XmlElement(name = "title", namespace = "http://bookservice.com/book/v1", required = true)
    private String title;

    @XmlElement(name = "author", namespace = "http://bookservice.com/book/v1", required = true)
    private String author;

    @XmlElement(name = "price", namespace = "http://bookservice.com/book/v1", required = true)
    private BigDecimal price;

    @XmlElement(name = "stockQuantity", namespace = "http://bookservice.com/book/v1", required = true)
    private Integer stockQuantity;

    @XmlElement(name = "genre", namespace = "http://bookservice.com/book/v1", required = false)
    private String genre;

    public BookType() {}

    public BookType(String isbn, String title, String author,
                    BigDecimal price, Integer stockQuantity, String genre) {
        this.isbn          = isbn;
        this.title         = title;
        this.author        = author;
        this.price         = price;
        this.stockQuantity = stockQuantity;
        this.genre         = genre;
    }

    public String     getIsbn()                          { return isbn; }
    public void       setIsbn(String isbn)               { this.isbn = isbn; }

    public String     getTitle()                         { return title; }
    public void       setTitle(String title)             { this.title = title; }

    public String     getAuthor()                        { return author; }
    public void       setAuthor(String author)           { this.author = author; }

    public BigDecimal getPrice()                         { return price; }
    public void       setPrice(BigDecimal price)         { this.price = price; }

    public Integer    getStockQuantity()                 { return stockQuantity; }
    public void       setStockQuantity(Integer qty)      { this.stockQuantity = qty; }

    public String     getGenre()                         { return genre; }
    public void       setGenre(String genre)             { this.genre = genre; }
}
