package se.chasacademy.library.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import se.chasacademy.library.integration.support.JwtTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Bucket4j-based per-IP rate limiting (RateLimitingFilter).
 *
 * Runs in its own Spring context (the custom rate.limit.capacity property
 * forces a dedicated context, separate from the one shared by the other
 * RANDOM_PORT integration tests) so its bucket state is never polluted by —
 * or polluting — unrelated tests.
 *
 * Capacity is set to 11 instead of 10: logging in inside @BeforeEach consumes
 * one token of the bucket before the test body's own 10-request loop runs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "rate.limit.capacity=11")
class RateLimitingIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = JwtTestSupport.withJwtAuth(restTemplate, "admin", "admin-changeit");
    }

    @Test
    @DisplayName("Rate Limiting — should return 429 after 10 requests in a row")
    void rateLimiting_shouldReturn429AfterLimit() {
        // Perform 10 valid requests (the login above already used 1 of the 11 tokens)
        for (int i = 0; i < 10; i++) {
            restTemplate.getForEntity("/api/v1/books", String.class);
        }

        // The next request should trigger the rate limit
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/books", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).contains("Too many requests");
    }
}
