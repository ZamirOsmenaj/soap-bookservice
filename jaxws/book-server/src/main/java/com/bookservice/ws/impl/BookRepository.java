package com.bookservice.ws.impl;

import com.bookservice.ws.model.BookType;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

// =============================================================================
// BookRepository — in-memory book catalog
// =============================================================================
// This simulates a database layer. In production this would be a JPA repository,
// a JDBC DAO, or a call to an external system.
//
// It is intentionally separated from the service implementation to demonstrate
// proper layering even in this demo:
//   Handler (auth) → PortType Impl (orchestration) → Repository (data access)
// =============================================================================
public class BookRepository {

    // Singleton — one shared catalog across both service endpoints
    private static final BookRepository INSTANCE = new BookRepository();

    // ISBN → BookType map
    private final Map<String, BookType> catalog = new LinkedHashMap<String, BookType>();

    private BookRepository() {
        // Pre-populate with some well-known books
        addBook(new BookType("978-0-13-468599-1", "Effective Java",
                "Joshua Bloch", new BigDecimal("49.99"), 15, "Programming"));

        addBook(new BookType("978-0-13-110362-7", "The Pragmatic Programmer",
                "David Thomas, Andrew Hunt", new BigDecimal("52.99"), 30, "Programming"));

        addBook(new BookType("978-0-596-51774-8", "JavaScript: The Good Parts",
                "Douglas Crockford", new BigDecimal("29.99"), 8, "Programming"));
    }

    public static BookRepository getInstance() {
        return INSTANCE;
    }

    public BookType findByIsbn(String isbn) {
        return catalog.get(isbn);
    }

    public boolean existsByIsbn(String isbn) {
        return catalog.containsKey(isbn);
    }

    public void addBook(BookType book) {
        catalog.put(book.getIsbn(), book);
    }

    // Generate a simple sequential book ID for newly created books
    public String generateBookId() {
        return "BK-" + String.format("%05d", catalog.size() + 1);
    }
}
