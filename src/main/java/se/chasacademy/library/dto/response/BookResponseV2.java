package se.chasacademy.library.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for API v2 Book responses.
 * Extends v1 structure with 'available' field.
 * 'authorName' mapped from the Author entity.
 */
@Schema(description = "Book response payload (v2) — includes availability status")
public class BookResponseV2 {

    @Schema(description = "Unique identifier of the book", example = "1")
    private Long id;

    @Schema(description = "Title of the book", example = "Clean Code")
    private String title;

    @Schema(description = "Name of the book's author", example = "Robert C. Martin")
    private String authorName;

    @Schema(description = "ID of the associated Author entity", example = "1")
    private Long authorId;

    @Schema(description = "Whether the book is currently available for borrowing", example = "true")
    private boolean available;

    // ── Constructors ──────────────────────────────────────────────────────────

    public BookResponseV2() {}

    public BookResponseV2(Long id, String title, String authorName, Long authorId, boolean available) {
        this.id = id;
        this.title = title;
        this.authorName = authorName;
        this.authorId = authorId;
        this.available = available;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
