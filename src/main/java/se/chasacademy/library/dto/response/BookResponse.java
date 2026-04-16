package se.chasacademy.library.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for API v1 Book responses.
 * Does NOT include the 'available' field — that is a v2 feature.
 */
@Schema(description = "Book response payload (v1)")
public class BookResponse {

    @Schema(description = "Unique identifier of the book", example = "1")
    private Long id;

    @Schema(description = "Title of the book", example = "Clean Code")
    private String title;

    @Schema(description = "Author of the book", example = "Robert C. Martin")
    private String author;

    // ── Constructors ──────────────────────────────────────────────────────────

    public BookResponse() {}

    public BookResponse(Long id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
}
