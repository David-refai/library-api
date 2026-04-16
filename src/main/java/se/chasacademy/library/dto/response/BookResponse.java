package se.chasacademy.library.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for API v1 Book responses.
 * 'authorName' is mapped from Author.name — the Author entity is never exposed directly.
 */
@Schema(description = "Book response payload (v1)")
public class BookResponse {

    @Schema(description = "Unique identifier of the book", example = "1")
    private Long id;

    @Schema(description = "Title of the book", example = "Clean Code")
    private String title;

    @Schema(description = "Name of the book's author", example = "Robert C. Martin")
    private String authorName;

    @Schema(description = "ID of the associated Author entity", example = "1")
    private Long authorId;

    // ── Constructors ──────────────────────────────────────────────────────────

    public BookResponse() {}

    public BookResponse(Long id, String title, String authorName, Long authorId) {
        this.id = id;
        this.title = title;
        this.authorName = authorName;
        this.authorId = authorId;
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
}
