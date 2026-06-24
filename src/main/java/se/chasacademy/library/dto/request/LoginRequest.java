package se.chasacademy.library.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for authenticating and obtaining a JWT.
 * Received in the request body of POST /api/v1/auth/login.
 */
@Schema(description = "Request payload for logging in")
public class LoginRequest {

    @NotBlank(message = "Username must not be blank")
    @Schema(description = "Account username", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password must not be blank")
    @Schema(description = "Account password", example = "admin-changeit", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    // ── Constructors ──────────────────────────────────────────────────────────

    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
