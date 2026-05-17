package com.bookservice.ws.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.math.BigDecimal;

// =============================================================================
// BookType — the core domain object
// =============================================================================
// Maps to <xsd:complexType name="BookType"> in book.xsd.
//
// This class is shared by both operations:
//   GetBook:    server populates a BookType and returns it in GetBookOutput
//   CreateBook: client sends a BookType inside CreateBookInput
//
// Java ↔ XSD type mappings used here:
//   xsd:string  → java.lang.String
//   xsd:decimal → java.math.BigDecimal  (NOT double — BigDecimal is exact)
//   xsd:int     → java.lang.Integer     (or primitive int)
// =============================================================================
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name      = "BookType",
    namespace = "http://bookservice.com/book/v1",
    // propOrder must match the order of elements in the XSD <xsd:sequence>
    propOrder = { "isbn", "title", "author", "price", "stockQuantity", "genre" }
)
public class BookType {

    @XmlElement(name = "isbn", namespace = "http://bookservice.com/book/v1", required = true)
    private String isbn;

    @XmlElement(name = "title", namespace = "http://bookservice.com/book/v1", required = true)
    private String title;

    @XmlElement(name = "author", namespace = "http://bookservice.com/book/v1", required = true)
    private String author;

    // =============================================================================
    // BigDecimal for monetary values
    // =============================================================================
    // xsd:decimal maps to java.math.BigDecimal.
    // NEVER use double/float for money — floating-point arithmetic is imprecise:
    //   0.1 + 0.2 = 0.30000000000000004  (floating point error!)
    //   new BigDecimal("0.1").add(new BigDecimal("0.2")) = 0.3  (exact!)
    // =============================================================================
    @XmlElement(name = "price", namespace = "http://bookservice.com/book/v1", required = true)
    private BigDecimal price;

    @XmlElement(name = "stockQuantity", namespace = "http://bookservice.com/book/v1", required = true)
    private Integer stockQuantity;

    // required = false → matches minOccurs="0" in the XSD (optional field)
    @XmlElement(name = "genre", namespace = "http://bookservice.com/book/v1", required = false)
    private String genre;

    // No-arg constructor required by JAXB
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

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer qty) { this.stockQuantity = qty; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
}
