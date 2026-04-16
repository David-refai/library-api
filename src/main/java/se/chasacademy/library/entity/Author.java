package se.chasacademy.library.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity representing an Author.
 *
 * Relation:
 *   Author (1) ──→ (*) Book   [@OneToMany, mappedBy = "author"]
 *
 * Author owns the inverse side of the relation — Book owns the FK (author_id).
 * This entity is NEVER exposed directly in API responses — use AuthorResponse DTO.
 */
@Entity
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * One Author can have many Books.
     * mappedBy = "author" means Book.author field owns the FK column.
     * CascadeType.ALL is intentionally NOT used — books managed independently.
     */
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    public Author() {}

    public Author(String name) {
        this.name = name;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Book> getBooks() { return books; }
    public void setBooks(List<Book> books) { this.books = books; }
}
