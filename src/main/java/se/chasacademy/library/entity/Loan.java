package se.chasacademy.library.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * JPA Entity representing a Loan (utlåning).
 *
 * Relation:
 *   Loan (1) ──→ (1) Book   [@OneToOne, unique constraint on book_id]
 *
 * Business rule enforced in LoanService:
 *   A Book can only have ONE active Loan. Attempting a second loan throws BookAlreadyOnLoanException.
 *
 * This entity is NEVER exposed directly — use LoanResponse DTO.
 */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The book being loaned.
     * unique = true enforces at DB level that one book can only appear in one loan row.
     */
    @OneToOne
    @JoinColumn(name = "book_id", nullable = false, unique = true)
    private Book book;

    /**
     * Set automatically to today's date when the loan is created.
     */
    @Column(nullable = false)
    private LocalDate loanDate;

    /**
     * Null while the book is still on loan.
     * Set when the book is returned (future endpoint).
     */
    @Column
    private LocalDate returnDate;

    @PrePersist
    protected void onCreate() {
        this.loanDate = LocalDate.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public Loan() {}

    public Loan(Book book) {
        this.book = book;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public LocalDate getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDate loanDate) { this.loanDate = loanDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
}
