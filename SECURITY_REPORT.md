# Security Report — Library API

This report documents the OWASP Top 10 security review carried out on the Library API
as part of Individuell Labb 2k5, and the three vulnerabilities that were identified
and fixed.

## Summary

| # | OWASP Category | Where it lived | Fixed in |
|---|---|---|---|
| 1 | A07:2021 — Identification and Authentication Failures | `SecurityConfig.java` | JWT authentication |
| 2 | A01:2021 — Broken Access Control | `SecurityConfig.java` | Deny-by-default + role-based authorization |
| 3 | A06:2021 — Vulnerable and Outdated Components | `pom.xml` | OWASP Dependency-Check, wired into CI |

---

## 1. A07 — Identification and Authentication Failures

### Identification
The original `SecurityConfig` used HTTP Basic Auth with a single in-memory account:

```java
UserDetails user = User.builder()
    .username("admin")
    .password("{noop}password")   // "{noop}" = no hashing at all
    .roles("ADMIN")
    .build();
```

Two distinct problems here, both falling under A07:
- The password was stored and compared **in plaintext** (`{noop}` explicitly
  disables hashing) — anyone with read access to the source or a memory dump
  gets the credential for free.
- The credential was **hardcoded in source control**, with no way to rotate it
  without a code change and redeploy.
- Basic Auth also means the raw password is sent on *every single request*,
  widening the window in which it can be intercepted or logged.

### Mitigation
- Replaced Basic Auth with **stateless JWT authentication**:
  `POST /api/v1/auth/login` authenticates via Spring's `AuthenticationManager`
  and returns a signed (HS256) token; all other endpoints require
  `Authorization: Bearer <token>` (`JwtAuthenticationFilter`, `JwtService`).
- Passwords are now **BCrypt-hashed** (`PasswordEncoder` bean in
  `SecurityConfig`) instead of stored in plaintext.
- Credentials and the signing secret are **externalized to environment
  variables** (`JWT_SECRET`, `ADMIN_PASSWORD`, `USER_PASSWORD`, ...) with
  development-only fallback defaults — see `application.properties` and the
  Dockerfile/CI workflow, which inject the real values via `-e` / GitHub
  Secrets, never via source code.
- Failed logins and missing/invalid tokens return a consistent `401` (via
  `RestAuthenticationEntryPoint` and a new `AuthenticationException` handler
  in `GlobalExceptionHandler`) instead of leaking stack traces or framework
  default error pages.

### Analysis & Prioritization
This was prioritized first because authentication is the foundation every
other control depends on — if it's broken, nothing downstream matters. A
hardcoded, unhashed password is the kind of vulnerability that shows up
verbatim in any `git log` or decompiled jar; if this API had been processing
real loans (or, in a paid-API scenario, billed per request), a leaked
credential would let an attacker authenticate indefinitely as `admin` and run
up costs or corrupt data with no way to revoke access short of redeploying
new code. Moving to short-lived, externally-configured JWTs means a leaked
secret can be rotated without a code change, and a compromised token expires
within an hour instead of working forever.

---

## 2. A01 — Broken Access Control

### Identification
The original authorization rule set:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**").permitAll()
    .requestMatchers("/api/**").authenticated()
    .anyRequest().permitAll()   // <-- default-allow fallback
)
```

Two problems:
- `anyRequest().permitAll()` is a **default-allow** policy: any endpoint added
  later that isn't explicitly matched is open to the entire internet by
  default, rather than the secure default of "deny unless explicitly
  allowed."
- Every `/api/**` rule only required `.authenticated()` — there was no
  distinction between roles. The single `admin` account could read *and*
  write, but so could any other authenticated account, present or future.
  There was no way to issue a "read-only" credential (e.g. for a partner
  integration or a junior team member) without giving them full write access.

### Mitigation
- Replaced the fallback with **`anyRequest().authenticated()`** — deny by
  default; only the login endpoint and API documentation are explicitly
  public.
- Split authorization **by HTTP method**: `GET` on `/api/v1/**` and
  `/api/v2/**` requires any authenticated account; `POST`/`PUT`/`PATCH`/`DELETE`
  require `hasRole("ADMIN")`. The `user` account can browse the catalog but
  cannot create, edit, or delete books, authors, or loans.
- Unauthorized write attempts now return a structured `403` via
  `RestAccessDeniedHandler`, verified by `AuthIntegrationTest` (a `USER`-role
  token gets `403` on `POST /api/v1/authors`; an `ADMIN`-role token gets
  `201`).

### Analysis & Prioritization
Broken access control is consistently the #1 issue in OWASP's own Top 10 data,
and the default-allow fallback here is exactly the failure mode that lets a
forgotten or newly-added endpoint slip through unauthenticated. Combined with
no role separation, *any* valid login — however it was obtained — was
equivalent to full admin access. In a real deployment, this means a single
leaked low-privilege credential (e.g. a read-only integration key) would be
enough to delete the entire book catalog. Fixing this was prioritized
alongside the authentication fix because the two are complementary: JWT
authentication establishes *who* you are, but without role-based
authorization it doesn't constrain *what* you're allowed to do once
authenticated.

---

## 3. A06 — Vulnerable and Outdated Components

### Identification
The project pulls in a sizeable dependency tree (Spring Security, Spring Data
Redis, Spring Cloud Vault, Resilience4j, Bucket4j, jjwt, springdoc-openapi,
...) with **no automated process** to detect when one of those libraries (or
their own transitive dependencies) has a disclosed CVE. Without this, a
critical vulnerability in, say, a JSON parsing library could sit unnoticed
indefinitely — the team would only find out by reading security mailing lists
manually, if at all.

### Mitigation
- Added the **OWASP Dependency-Check Maven plugin** (`pom.xml`), configured
  with `failBuildOnCVSS=7` — any dependency with a High or Critical severity
  vulnerability fails the build.
- Deliberately **not bound to the default Maven lifecycle**, so `mvn test`
  stays fast for everyday development; instead it's invoked explicitly as its
  own `dependency-check` job in `ci-cd-pipeline.yml`, which runs on every push
  and pull request and **blocks** the `docker-build-push` job from running if
  it fails — a vulnerable build can never reach Docker Hub.
- The HTML/JSON report is uploaded as a build artifact so a human can review
  exactly which dependency and which CVE triggered a failure.

### Analysis & Prioritization
CVSS 7 (High) was chosen deliberately over a stricter Critical-only (CVSS 9)
threshold: a High-severity flaw in a dependency this API actually exercises at
runtime (e.g. Spring Security or Jackson, both directly in the request path)
is a realistic exploitation risk, not just a theoretical one, and the cost of
occasionally re-evaluating a noisy finding is far lower than the cost of
shipping a known-vulnerable library to production. This was ranked third
because, unlike the authentication and access-control issues, it isn't a
vulnerability in code the team wrote — it's a vulnerability in code the team
*depends on*, and the size of the dependency list (six third-party libraries
added in this lab alone) makes manual tracking impractical. Automating the
check in CI means the team finds out about a newly-disclosed CVE on the very
next push, instead of finding out from an incident report.

---

## Bonus: Defense-in-depth (not one of the three required fixes)

`RateLimitingFilter` (Bucket4j) caps each client IP to a configurable number
of requests per minute, returning `429 Too Many Requests` once exceeded. This
isn't one of the three vulnerabilities above, but it complements the
authentication fix: even with strong credentials, an unthrottled `/api/v1/auth/login`
endpoint would otherwise be open to unlimited brute-force password guessing.
