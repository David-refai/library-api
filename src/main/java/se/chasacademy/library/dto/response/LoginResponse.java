package se.chasacademy.library.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO returned after a successful login — carries the JWT to use as a
 * Bearer token on subsequent requests.
 */
@Schema(description = "Login response payload containing the JWT")
public class LoginResponse {

    @Schema(description = "JWT to send as 'Authorization: Bearer <token>' on subsequent requests")
    private String token;

    @Schema(description = "Token lifetime in milliseconds", example = "3600000")
    private long expiresInMs;

    // ── Constructors ──────────────────────────────────────────────────────────

    public LoginResponse() {}

    public LoginResponse(String token, long expiresInMs) {
        this.token = token;
        this.expiresInMs = expiresInMs;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public long getExpiresInMs() { return expiresInMs; }
    public void setExpiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; }
}
