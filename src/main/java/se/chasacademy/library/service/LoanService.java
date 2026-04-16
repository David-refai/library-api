package se.chasacademy.library.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.chasacademy.library.dto.request.LoanRequest;
import se.chasacademy.library.dto.response.LoanResponse;
import se.chasacademy.library.entity.Book;
import se.chasacademy.library.entity.Loan;
import se.chasacademy.library.exception.BookAlreadyOnLoanException;
import se.chasacademy.library.exception.BookNotFoundException;
import se.chasacademy.library.repository.BookRepository;
import se.chasacademy.library.repository.LoanRepository;

import java.util.List;

/**
 * Service layer for all Loan business logic.
 * Controllers ONLY call this class — never the Repository directly.
 *
 * Business rule: a Book can only have ONE active loan at a time.
 */
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
    }

    /**
     * Creates a new loan for the given bookId.
     *
     * @Transactional ensures the check-and-save operation is atomic.
     * Without this, two concurrent requests could both pass the "already on loan" check
     * and both create a loan — a classic race condition.
     *
     * With @Transactional, the database transaction is held open for the duration of this method,
     * and the unique constraint on book_id in the loans table acts as a final safety net.
     *
     * @throws BookNotFoundException      if no book with the given ID exists
     * @throws BookAlreadyOnLoanException if the book is already on loan
     */
    @Transactional
    public LoanResponse createLoan(LoanRequest request) {
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new BookNotFoundException(request.getBookId()));

        // Business rule: reject if an active loan already exists for this book
        boolean alreadyOnLoan = loanRepository.findByBookId(book.getId()).isPresent();
        if (alreadyOnLoan) {
            throw new BookAlreadyOnLoanException(book.getId());
        }

        Loan loan = new Loan(book);
        Loan saved = loanRepository.save(loan);
        return toResponse(saved);
    }

    /**
     * Retrieves all active loans.
     */
    public List<LoanResponse> getAllLoans() {
        return loanRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Private Mapping Helpers ───────────────────────────────────────────────

    private LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getBook().getAuthor().getName(),
                loan.getLoanDate(),
                loan.getReturnDate()
        );
    }
}
