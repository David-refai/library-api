package se.chasacademy.library.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Standardized error response body returned for all error scenarios.
 *
 * Example:
 * {
 *   "timestamp": "2025-09-01T10:00:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Book with id 99 not found",
 *   "path": "/api/v1/books/99"
 * }
 */
@Schema(description = "Standardized error response returned for all API errors")
public class ApiError {

    @Schema(description = "Timestamp when the error occurred", example = "2025-09-01T10:00:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "HTTP error name", example = "Not Found")
    private String error;

    @Schema(description = "Detailed error message", example = "Book with id 99 not found")
    private String message;

    @Schema(description = "Request path that triggered the error", example = "/api/v1/books/99")
    private String path;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ApiError() {}

    public ApiError(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
