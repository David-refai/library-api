package se.chasacademy.library.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import se.chasacademy.library.dto.request.AuthorRequest;
import se.chasacademy.library.dto.request.LoginRequest;
import se.chasacademy.library.dto.response.AuthorResponse;
import se.chasacademy.library.dto.response.LoginResponse;
import se.chasacademy.library.exception.ApiError;
import se.chasacademy.library.integration.support.JwtTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JWT authentication and role-based access control
 * (OWASP A07 and A01 fixes).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * restTemplate is a context-cached bean shared with other test classes, which may
     * have left a Bearer-token interceptor attached from their own withJwtAuth(...) calls.
     * Clear it so every test here starts from a guaranteed unauthenticated state.
     */
    @BeforeEach
    void clearStaleAuth() {
        restTemplate.getRestTemplate().getInterceptors().clear();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — valid credentials should return 200 and a JWT")
    void login_withValidCredentials_shouldReturnToken() {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest("admin", "admin-changeit"), LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — invalid credentials should return 401")
    void login_withInvalidCredentials_shouldReturn401() {
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest("admin", "wrong-password"), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /api/v1/authors — without a token should return 401")
    void protectedEndpoint_withoutToken_shouldReturn401() {
        ResponseEntity<ApiError> response = restTemplate.getForEntity("/api/v1/authors", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /api/v1/authors — USER role should return 403 (only ADMIN may write)")
    void mutatingEndpoint_withUserRole_shouldReturn403() {
        TestRestTemplate userTemplate = JwtTestSupport.withJwtAuth(restTemplate, "user", "user-changeit");

        ResponseEntity<ApiError> response = userTemplate.postForEntity(
                "/api/v1/authors", new AuthorRequest("Forbidden Author"), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("POST /api/v1/authors — ADMIN role should return 201")
    void mutatingEndpoint_withAdminRole_shouldReturn201() {
        TestRestTemplate adminTemplate = JwtTestSupport.withJwtAuth(restTemplate, "admin", "admin-changeit");

        ResponseEntity<AuthorResponse> response = adminTemplate.postForEntity(
                "/api/v1/authors", new AuthorRequest("Allowed Author"), AuthorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
