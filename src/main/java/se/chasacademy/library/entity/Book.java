package se.chasacademy.library.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a Book in the database.
 *
 * Relations:
 *   Book (*) ──→ (1) Author   [@ManyToOne — Book owns the FK author_id]
 *   Book (1) ──→ (0..1) Loan  [@OneToOne, managed from Loan side]
 *
 * This entity is NEVER exposed directly in API responses — DTOs are used instead.
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /**
     * Many Books belong to one Author.
     * Book owns this relation — it holds the author_id FK column.
     * fetch = LAZY: Author data is only loaded when explicitly accessed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public Book() {}

    public Book(String title, Author author) {
        this.title = title;
        this.author = author;
        this.available = true;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
