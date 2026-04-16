package se.chasacademy.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.chasacademy.library.entity.Loan;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Loan entities.
 * Only accessed from LoanService — never from Controllers.
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    /**
     * Check if an active loan exists for a given book.
     * Used in LoanService to enforce the "one loan per book" business rule.
     *
     * @param bookId the ID of the book
     * @return Optional containing the loan if found
     */
    Optional<Loan> findByBookId(Long bookId);
}
