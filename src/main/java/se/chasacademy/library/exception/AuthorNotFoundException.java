package se.chasacademy.library.exception;

/**
 * Thrown when an Author with the given ID cannot be found in the database.
 * Caught by GlobalExceptionHandler and mapped to HTTP 404.
 */
public class AuthorNotFoundException extends RuntimeException {

    private final Long authorId;

    public AuthorNotFoundException(Long authorId) {
        super("Author with id " + authorId + " not found");
        this.authorId = authorId;
    }

    public Long getAuthorId() {
        return authorId;
    }
}
