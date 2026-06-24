package se.chasacademy.library.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.chasacademy.library.dto.request.LoginRequest;
import se.chasacademy.library.dto.response.LoginResponse;
import se.chasacademy.library.exception.ApiError;
import se.chasacademy.library.security.JwtService;

import java.util.Collection;

/**
 * Issues JWTs for the two in-memory accounts (admin/user).
 * Replaces HTTP Basic Auth as the API's authentication mechanism (OWASP A07 fix).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Login and obtain a JWT to authenticate further requests")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long expirationMs;

    public AuthController(AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           @Value("${jwt.expiration-ms}") long expirationMs) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.expirationMs = expirationMs;
    }

    @Operation(summary = "Log in and obtain a JWT",
            description = "Authenticates with username/password and returns a Bearer token to use on subsequent requests.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid username or password",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        String token = jwtService.generateToken(authentication.getName(), authorities);
        return ResponseEntity.ok(new LoginResponse(token, expirationMs));
    }
}
