package se.chasacademy.library.exception;

/**
 * Thrown when a loan is attempted on a Book that is already on loan.
 * Caught by GlobalExceptionHandler and mapped to HTTP 400 Bad Request.
 *
 * Business rule: one active loan per book at a time.
 */
public class BookAlreadyOnLoanException extends RuntimeException {

    private final Long bookId;

    public BookAlreadyOnLoanException(Long bookId) {
        super("Book is already on loan");
        this.bookId = bookId;
    }

    public Long getBookId() {
        return bookId;
    }
}
