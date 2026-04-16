package se.chasacademy.library.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * DTO for Loan responses.
 * Shows loan details without exposing Book or Loan entities directly.
 *
 * returnDate is null while the book is still on loan.
 */
@Schema(description = "Loan response payload")
public class LoanResponse {

    @Schema(description = "Unique identifier of the loan", example = "1")
    private Long id;

    @Schema(description = "ID of the loaned book", example = "5")
    private Long bookId;

    @Schema(description = "Title of the loaned book", example = "Clean Code")
    private String bookTitle;

    @Schema(description = "Name of the book's author", example = "Robert C. Martin")
    private String authorName;

    @Schema(description = "Date when the loan was created", example = "2025-09-01")
    private LocalDate loanDate;

    @Schema(description = "Date when the book was returned — null if still on loan", example = "null")
    private LocalDate returnDate;

    // ── Constructors ──────────────────────────────────────────────────────────

    public LoanResponse() {}

    public LoanResponse(Long id, Long bookId, String bookTitle, String authorName,
                        LocalDate loanDate, LocalDate returnDate) {
        this.id = id;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.authorName = authorName;
        this.loanDate = loanDate;
        this.returnDate = returnDate;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public LocalDate getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDate loanDate) { this.loanDate = loanDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
}
