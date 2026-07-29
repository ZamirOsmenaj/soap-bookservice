package com.bookservice.springserver.repository;

import com.bookservice.springserver.model.BookType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

// =============================================================================
// BookRepository — in-memory book catalog
// =============================================================================
// Identical logic to the plain-Java version. The only difference is that
// it is a Spring @Repository bean (singleton managed by the Spring context)
// rather than a hand-rolled singleton with a static INSTANCE field.
//
// @Repository marks it as a Spring-managed component so it can be
// @Autowired into the endpoint classes. Spring creates exactly one instance
// (default singleton scope), so the catalog is shared across both endpoints —
// the same guarantee the plain-Java static singleton provided.
// =============================================================================
@Repository
public class BookRepository {

    // ISBN → BookType map (insertion-ordered for consistent listing)
    private final Map<String, BookType> catalog = new LinkedHashMap<>();

    public BookRepository() {
        // Pre-populate with the same books as the plain-Java version
        addBook(new BookType("978-0-13-468599-1", "Effective Java",
                "Joshua Bloch", new BigDecimal("49.99"), 15, "Programming"));

        addBook(new BookType("978-0-13-110362-7", "The Pragmatic Programmer",
                "David Thomas, Andrew Hunt", new BigDecimal("52.99"), 30, "Programming"));

        addBook(new BookType("978-0-596-51774-8", "JavaScript: The Good Parts",
                "Douglas Crockford", new BigDecimal("29.99"), 8, "Programming"));
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

    public String generateBookId() {
        return "BK-" + String.format("%05d", catalog.size() + 1);
    }
}
