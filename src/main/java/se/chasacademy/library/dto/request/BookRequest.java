package se.chasacademy.library.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new Book.
 * Received in the request body of POST /api/v1/books.
 *
 * Note: 'author' is no longer a String — books are now linked to an Author entity via authorId.
 */
@Schema(description = "Request payload for creating a new book")
public class BookRequest {

    @NotBlank(message = "Title must not be blank")
    @Schema(description = "Title of the book", example = "Clean Code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotNull(message = "Author ID must not be null")
    @Schema(description = "ID of the existing Author to associate with this book", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long authorId;

    // ── Constructors ──────────────────────────────────────────────────────────

    public BookRequest() {}

    public BookRequest(String title, Long authorId) {
        this.title = title;
        this.authorId = authorId;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
}
