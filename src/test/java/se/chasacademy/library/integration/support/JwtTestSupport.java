package se.chasacademy.library.integration.support;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import se.chasacademy.library.dto.request.LoginRequest;
import se.chasacademy.library.dto.response.LoginResponse;

/**
 * Test helper that mirrors {@code TestRestTemplate.withBasicAuth(...)} for
 * JWT: logs in via POST /api/v1/auth/login and attaches the returned token
 * as a Bearer Authorization header on every subsequent request made with
 * the given {@link TestRestTemplate}.
 */
public final class JwtTestSupport {

    private JwtTestSupport() {}

    public static TestRestTemplate withJwtAuth(TestRestTemplate restTemplate, String username, String password) {
        // Drop any interceptor left over from a previous call (e.g. a prior test method
        // re-using the same context-cached TestRestTemplate bean).
        restTemplate.getRestTemplate().getInterceptors().clear();

        LoginResponse loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(username, password), LoginResponse.class
        ).getBody();

        String token = loginResponse.getToken();

        restTemplate.getRestTemplate().getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return execution.execute(request, body);
        });

        return restTemplate;
    }
}
