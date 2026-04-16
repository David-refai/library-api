package se.chasacademy.library.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating a new Author.
 * Received in the request body of POST /api/v1/authors.
 */
@Schema(description = "Request payload for creating a new author")
public class AuthorRequest {

    @NotBlank(message = "Author name must not be blank")
    @Schema(description = "Full name of the author", example = "Robert C. Martin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    // ── Constructors ──────────────────────────────────────────────────────────

    public AuthorRequest() {}

    public AuthorRequest(String name) {
        this.name = name;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
