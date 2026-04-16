package se.chasacademy.library.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating a new Book.
 * Received in the request body of POST /api/v1/books.
 */
@Schema(description = "Request payload for creating a new book")
public class BookRequest {

    @NotBlank(message = "Title must not be blank")
    @Schema(description = "Title of the book", example = "Clean Code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "Author must not be blank")
    @Schema(description = "Author of the book", example = "Robert C. Martin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String author;

    // ── Constructors ──────────────────────────────────────────────────────────

    public BookRequest() {}

    public BookRequest(String title, String author) {
        this.title = title;
        this.author = author;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
}
