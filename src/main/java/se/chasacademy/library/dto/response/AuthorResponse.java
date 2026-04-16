package se.chasacademy.library.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for Author responses.
 * Includes 'bookCount' to show how many books the author has — without exposing the Book entity.
 */
@Schema(description = "Author response payload")
public class AuthorResponse {

    @Schema(description = "Unique identifier of the author", example = "1")
    private Long id;

    @Schema(description = "Full name of the author", example = "Robert C. Martin")
    private String name;

    @Schema(description = "Number of books by this author in the library", example = "3")
    private int bookCount;

    // ── Constructors ──────────────────────────────────────────────────────────

    public AuthorResponse() {}

    public AuthorResponse(Long id, String name, int bookCount) {
        this.id = id;
        this.name = name;
        this.bookCount = bookCount;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getBookCount() { return bookCount; }
    public void setBookCount(int bookCount) { this.bookCount = bookCount; }
}
