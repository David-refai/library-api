package se.chasacademy.library.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.chasacademy.library.exception.*;

import java.util.stream.Collectors;

/**
 * Global exception handler — catches all exceptions thrown from controllers
 * and returns a standardized {@link ApiError} response.
 *
 * Stack traces are NEVER exposed to API consumers.
 * Uses @RestControllerAdvice so the response is automatically serialized to JSON.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles BookNotFoundException → HTTP 404 Not Found.
     */
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ApiError> handleBookNotFoundException(
            BookNotFoundException ex, HttpServletRequest request) {

        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * Handles AuthorNotFoundException → HTTP 404 Not Found.
     */
    @ExceptionHandler(AuthorNotFoundException.class)
    public ResponseEntity<ApiError> handleAuthorNotFoundException(
            AuthorNotFoundException ex, HttpServletRequest request) {

        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * Handles BookAlreadyOnLoanException → HTTP 400 Bad Request.
     */
    @ExceptionHandler(BookAlreadyOnLoanException.class)
    public ResponseEntity<ApiError> handleBookAlreadyOnLoanException(
            BookAlreadyOnLoanException ex, HttpServletRequest request) {

        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Handles DataIntegrityViolationException → HTTP 409 Conflict.
     *
     * This fires when the DB-level unique constraint on loans(book_id) is violated.
     * This happens in concurrent scenarios where two requests both pass the
     * service-level check before either commits — the DB unique index is the
     * final safety net that prevents duplicate loans.
     *
     * Mapped to 409 Conflict (not 400) to distinguish from validation errors.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        return buildError(HttpStatus.CONFLICT, "Book is already on loan (Data Integrity Violation)", request);
    }

    /**
     * Handles ObjectOptimisticLockingFailureException → HTTP 409 Conflict.
     *
     * Vecka 7: This fires when Optimistic Locking (@Version) catches two concurrent 
     * threads trying to update the same Book entity at the same time.
     * The first thread wins, the second throws this exception.
     */
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLockingFailure(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {

        return buildError(HttpStatus.CONFLICT, "Book is already on loan (Optimistic Lock Failure)", request);
    }

    /**
     * Handles @Valid / @Validated failures → HTTP 400 Bad Request.
     * Collects all field validation messages into a single comma-separated string.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return buildError(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Catch-all handler for any unexpected exception → HTTP 500.
     * Ensures stack traces never leak to API consumers.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex, HttpServletRequest request) {

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request
        );
    }

    // ── Private Helper ────────────────────────────────────────────────────────

    private ResponseEntity<ApiError> buildError(HttpStatus status, String message, HttpServletRequest request) {
        ApiError error = new ApiError(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }
}
