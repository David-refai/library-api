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
import se.chasacademy.library.dto.request.BookRequest;
import se.chasacademy.library.dto.response.AuthorResponse;
import se.chasacademy.library.dto.response.BookResponse;
import se.chasacademy.library.exception.ApiError;
import se.chasacademy.library.repository.AuthorRepository;
import se.chasacademy.library.repository.BookRepository;
import se.chasacademy.library.repository.LoanRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Author endpoints — Övning 1 & 2.
 *
 * Covers:
 *  - POST /api/v1/authors  (happy path + 400 validation)
 *  - GET  /api/v1/authors/{id}  (200 + 404)
 *  - GET  /api/v1/authors/{id}/books  (with books + empty list + 404)
 *
 * Rules:
 *  - @SpringBootTest RANDOM_PORT — full HTTP stack, no mocks
 *  - @BeforeEach clears all tables — tests are fully independent
 *  - TestRestTemplate for all calls
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthorIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private LoanRepository loanRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private AuthorRepository authorRepository;

    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }

    // ── POST /api/v1/authors — Happy Path ─────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/authors — 201 Created with correct body")
    void createAuthor_shouldReturn201WithBody() {
        ResponseEntity<AuthorResponse> response = restTemplate.postForEntity(
                "/api/v1/authors",
                new AuthorRequest("Robert C. Martin"),
                AuthorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AuthorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull().isPositive();
        assertThat(body.getName()).isEqualTo("Robert C. Martin");
        assertThat(body.getBookCount()).isZero(); // no books yet
        assertThat(authorRepository.count()).isEqualTo(1);
    }

    // ── POST /api/v1/authors — Validation: blank name → 400 ─────────────────

    @Test
    @DisplayName("POST /api/v1/authors — 400 when name is blank")
    void createAuthor_blankName_shouldReturn400() {
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/v1/authors",
                new AuthorRequest(""),
                ApiError.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getError()).isEqualTo("Bad Request");
        assertThat(error.getMessage()).containsIgnoringCase("name");
        assertThat(error.getPath()).isEqualTo("/api/v1/authors");
        assertThat(error.getTimestamp()).isNotNull();
        // No author should be saved
        assertThat(authorRepository.count()).isZero();
    }

    // ── POST — null name → 400 ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/authors — 400 when name is null")
    void createAuthor_nullName_shouldReturn400() {
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/v1/authors",
                new AuthorRequest(null),
                ApiError.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    // ── GET /api/v1/authors/{id} — Happy Path ────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/authors/{id} — 200 with correct author data")
    void getAuthorById_shouldReturn200WithAuthor() {
        // Create author
        Long id = createAuthor("Martin Fowler");

        ResponseEntity<AuthorResponse> response = restTemplate.getForEntity(
                "/api/v1/authors/" + id, AuthorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(id);
        assertThat(body.getName()).isEqualTo("Martin Fowler");
        assertThat(body.getBookCount()).isZero();
    }

    // ── GET /api/v1/authors/{id} — Not Found → 404 ──────────────────────────

    @Test
    @DisplayName("GET /api/v1/authors/{id} — 404 when author does not exist")
    void getAuthorById_notFound_shouldReturn404() {
        ResponseEntity<ApiError> response = restTemplate.getForEntity(
                "/api/v1/authors/99999", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiError error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getError()).isEqualTo("Not Found");
        assertThat(error.getMessage()).contains("99999");
        assertThat(error.getPath()).isEqualTo("/api/v1/authors/99999");
    }

    // ── GET /api/v1/authors/{id}/books — With Books ──────────────────────────

    @Test
    @DisplayName("GET /api/v1/authors/{id}/books — 200 with books list")
    void getBooksByAuthor_shouldReturn200WithBooks() {
        Long authorId = createAuthor("Joshua Bloch");

        // Create two books for this author
        createBook("Effective Java", authorId);
        createBook("Java Puzzlers", authorId);

        ResponseEntity<BookResponse[]> response = restTemplate.getForEntity(
                "/api/v1/authors/" + authorId + "/books", BookResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BookResponse[] books = response.getBody();
        assertThat(books).isNotNull().hasSize(2);
        assertThat(books).extracting(BookResponse::getAuthorName)
                .containsOnly("Joshua Bloch");
        assertThat(books).extracting(BookResponse::getTitle)
                .containsExactlyInAnyOrder("Effective Java", "Java Puzzlers");
    }

    // ── GET /api/v1/authors/{id}/books — Empty List (Edge Case) ─────────────

    @Test
    @DisplayName("GET /api/v1/authors/{id}/books — 200 with empty list when author has no books")
    void getBooksByAuthor_noBooks_shouldReturn200EmptyList() {
        Long authorId = createAuthor("New Author With No Books");

        ResponseEntity<BookResponse[]> response = restTemplate.getForEntity(
                "/api/v1/authors/" + authorId + "/books", BookResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    // ── GET /api/v1/authors/{id}/books — Author Not Found → 404 ─────────────

    @Test
    @DisplayName("GET /api/v1/authors/{id}/books — 404 when author does not exist")
    void getBooksByAuthor_authorNotFound_shouldReturn404() {
        ResponseEntity<ApiError> response = restTemplate.getForEntity(
                "/api/v1/authors/88888/books", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).contains("88888");
    }

    // ── GET — bookCount increments correctly ─────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/authors/{id} — bookCount reflects actual books")
    void getAuthorById_bookCountReflectsBooks() {
        Long authorId = createAuthor("Donald Knuth");
        createBook("The Art of Computer Programming Vol 1", authorId);
        createBook("The Art of Computer Programming Vol 2", authorId);
        createBook("The Art of Computer Programming Vol 3", authorId);

        ResponseEntity<AuthorResponse> response = restTemplate.getForEntity(
                "/api/v1/authors/" + authorId, AuthorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getBookCount()).isEqualTo(3);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Long createAuthor(String name) {
        ResponseEntity<AuthorResponse> r = restTemplate.postForEntity(
                "/api/v1/authors", new AuthorRequest(name), AuthorResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().getId();
    }

    private void createBook(String title, Long authorId) {
        ResponseEntity<BookResponse> r = restTemplate.postForEntity(
                "/api/v1/books", new BookRequest(title, authorId), BookResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
