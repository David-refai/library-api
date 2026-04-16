package se.chasacademy.library.exception;

/**
 * Thrown when a Book with the given ID cannot be found in the database.
 * Caught by GlobalExceptionHandler and mapped to HTTP 404.
 */
public class BookNotFoundException extends RuntimeException {

    private final Long bookId;

    public BookNotFoundException(Long bookId) {
        super("Book with id " + bookId + " not found");
        this.bookId = bookId;
    }

    public Long getBookId() {
        return bookId;
    }
}
