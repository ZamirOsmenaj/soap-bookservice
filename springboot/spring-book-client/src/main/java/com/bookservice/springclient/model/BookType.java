package com.bookservice.springclient.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name      = "BookType",
    namespace = "http://bookservice.com/book/v1",
    propOrder = { "isbn", "title", "author", "price", "stockQuantity", "genre" }
)
public class BookType {

    @XmlElement(name = "isbn",          namespace = "http://bookservice.com/book/v1", required = true)
    private String isbn;

    @XmlElement(name = "title",         namespace = "http://bookservice.com/book/v1", required = true)
    private String title;

    @XmlElement(name = "author",        namespace = "http://bookservice.com/book/v1", required = true)
    private String author;

    @XmlElement(name = "price",         namespace = "http://bookservice.com/book/v1", required = true)
    private BigDecimal price;

    @XmlElement(name = "stockQuantity", namespace = "http://bookservice.com/book/v1", required = true)
    private Integer stockQuantity;

    @XmlElement(name = "genre",         namespace = "http://bookservice.com/book/v1", required = false)
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

    public String     getIsbn()                     { return isbn; }
    public void       setIsbn(String isbn)          { this.isbn = isbn; }
    public String     getTitle()                    { return title; }
    public void       setTitle(String title)        { this.title = title; }
    public String     getAuthor()                   { return author; }
    public void       setAuthor(String author)      { this.author = author; }
    public BigDecimal getPrice()                    { return price; }
    public void       setPrice(BigDecimal price)    { this.price = price; }
    public Integer    getStockQuantity()            { return stockQuantity; }
    public void       setStockQuantity(Integer qty) { this.stockQuantity = qty; }
    public String     getGenre()                    { return genre; }
    public void       setGenre(String genre)        { this.genre = genre; }
}
