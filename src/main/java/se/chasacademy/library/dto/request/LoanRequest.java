package se.chasacademy.library.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new Loan.
 * Received in the request body of POST /api/v1/loans.
 *
 * Only bookId is needed — loanDate is set automatically in the entity.
 */
@Schema(description = "Request payload for creating a new loan")
public class LoanRequest {

    @NotNull(message = "Book ID must not be null")
    @Schema(description = "ID of the book to loan", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bookId;

    // ── Constructors ──────────────────────────────────────────────────────────

    public LoanRequest() {}

    public LoanRequest(Long bookId) {
        this.bookId = bookId;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
}
