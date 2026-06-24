package se.chasacademy.library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import se.chasacademy.library.security.JwtAuthenticationFilter;
import se.chasacademy.library.security.JwtService;
import se.chasacademy.library.security.RestAccessDeniedHandler;
import se.chasacademy.library.security.RestAuthenticationEntryPoint;

import java.util.List;

/**
 * Application security configuration.
 *
 * Fixes two OWASP Top 10 issues found in the original scaffold:
 *  - A07 (Identification and Authentication Failures): HTTP Basic Auth with
 *    a single hardcoded, plaintext ("{noop}") in-memory user is replaced
 *    with stateless JWT authentication and BCrypt-hashed credentials
 *    sourced from environment variables.
 *  - A01 (Broken Access Control): the previous "anyRequest().permitAll()"
 *    default-allow fallback, plus undifferentiated access for any
 *    authenticated user, is replaced with a default-deny policy and
 *    role-based authorization — only ADMIN can create/update/delete data.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(RestAuthenticationEntryPoint authenticationEntryPoint,
                           RestAccessDeniedHandler accessDeniedHandler) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Stateless JWT API — no browser session/cookies to protect against CSRF
            .cors(Customizer.withDefaults())
            // Stateless sessions: every request is authenticated solely via its JWT, never via a server-side session.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(handling -> handling
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                    // Public: login endpoint and API documentation.
                    .requestMatchers("/api/v1/auth/login", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**").permitAll()
                    // Reads: any authenticated account (admin or user).
                    .requestMatchers(HttpMethod.GET, "/api/v1/**", "/api/v2/**").authenticated()
                    // Writes: admin only.
                    .requestMatchers(HttpMethod.POST, "/api/v1/**", "/api/v2/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/**", "/api/v2/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/**", "/api/v2/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/**", "/api/v2/**").hasRole("ADMIN")
                    // Deny-by-default: anything not explicitly listed above requires authentication.
                    .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Two in-memory accounts for the lab's scope (no user-registration flow
     * is required by the assignment). Credentials come from environment
     * variables (see application.properties) and are BCrypt-hashed — fixing
     * the original "{noop}" plaintext-password storage.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder,
                                                  @Value("${app.security.admin-username}") String adminUsername,
                                                  @Value("${app.security.admin-password}") String adminPassword,
                                                  @Value("${app.security.user-username}") String userUsername,
                                                  @Value("${app.security.user-password}") String userPassword) {
        var admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();

        var user = User.builder()
                .username(userUsername)
                .password(passwordEncoder.encode(userPassword))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,PasswordEncoder passwordEncoder) {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080")); // Frontend origins
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
