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
     * @Transactional combined with @Version on Book (Optimistic Locking)
     * ensures thread-safety without heavy database locks.
     * When concurrent requests try to check out the same book, they will
     * both read version V. The first to commit updates version to V+1.
     * The second one will fail with ObjectOptimisticLockingFailureException.
     *
     * @throws BookNotFoundException      if no book with the given ID exists
     * @throws BookAlreadyOnLoanException if the book is already marked as not available
     */
    @Transactional
    public LoanResponse createLoan(LoanRequest request) {
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new BookNotFoundException(request.getBookId()));

        // Business rule: reject if book is not available
        if (!book.isAvailable()) {
            throw new BookAlreadyOnLoanException(book.getId());
        }

        // Mark book as unavailable. This updates the Book entity, 
        // triggering the @Version optimistic lock!
        book.setAvailable(false);
        bookRepository.save(book);

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
