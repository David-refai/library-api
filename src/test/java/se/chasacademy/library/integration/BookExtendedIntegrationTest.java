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
import se.chasacademy.library.dto.response.BookResponseV2;
import se.chasacademy.library.dto.response.PagedResponseV2;
import se.chasacademy.library.exception.ApiError;
import se.chasacademy.library.repository.AuthorRepository;
import se.chasacademy.library.repository.BookRepository;
import se.chasacademy.library.repository.LoanRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extended Book integration tests — covers GET all books, v2 endpoint,
 * author-not-found on create, and edge cases.
 *
 * Original happy-path tests live in BookIntegrationTest.
 * This class focuses on extra coverage scenarios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookExtendedIntegrationTest {

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

    // ── GET /api/v1/books — All Books ─────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/books — 200 with all books")
    void getAllBooks_shouldReturn200WithBooks() {
        Long authorId = createAuthor("Kent Beck");
        createBook("Test Driven Development", authorId);
        createBook("Extreme Programming Explained", authorId);

        ResponseEntity<BookResponse[]> response = restTemplate.getForEntity(
                "/api/v1/books", BookResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody())
                .extracting(BookResponse::getAuthorName)
                .containsOnly("Kent Beck");
    }

    // ── GET /api/v1/books — Empty List (Edge Case) ───────────────────────────

    @Test
    @DisplayName("GET /api/v1/books — 200 with empty list when no books")
    void getAllBooks_empty_shouldReturn200EmptyList() {
        ResponseEntity<BookResponse[]> response = restTemplate.getForEntity(
                "/api/v1/books", BookResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    // ── POST /api/v1/books — Author Not Found → 404 ──────────────────────────

    @Test
    @DisplayName("POST /api/v1/books — 404 when authorId does not exist")
    void createBook_authorNotFound_shouldReturn404() {
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/v1/books",
                new BookRequest("Orphaned Book", 99999L),
                ApiError.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiError error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getMessage()).contains("99999");
        assertThat(bookRepository.count()).isZero();
    }

    // ── GET /api/v2/books — V2 Response Format ───────────────────────────────

    @Test
    @DisplayName("GET /api/v2/books — 200 with v2 wrapper containing 'data' and 'version'")
    @SuppressWarnings("unchecked")
    void getAllBooksV2_shouldReturnWrapperWithVersionAndAvailable() {
        Long authorId = createAuthor("Brian Goetz");
        createBook("Java Concurrency in Practice", authorId);

        ResponseEntity<PagedResponseV2> response = restTemplate.getForEntity(
                "/api/v2/books", PagedResponseV2.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PagedResponseV2 body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getVersion()).isEqualTo("v2");
        assertThat(body.getData()).isNotNull().hasSize(1);
    }

    // ── GET /api/v2/books — Empty Data Array ─────────────────────────────────

    @Test
    @DisplayName("GET /api/v2/books — 200 with empty data array and version field")
    void getAllBooksV2_empty_shouldReturnWrapperWithEmptyData() {
        ResponseEntity<PagedResponseV2> response = restTemplate.getForEntity(
                "/api/v2/books", PagedResponseV2.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getVersion()).isEqualTo("v2");
        assertThat(response.getBody().getData()).isEmpty();
    }

    // ── v1 endpoints unaffected by v2 ────────────────────────────────────────

    @Test
    @DisplayName("v1 and v2 return independently — v1 response has no 'version' field")
    void v1AndV2_areIndependent() {
        Long authorId = createAuthor("Grady Booch");
        createBook("Object Oriented Analysis and Design", authorId);

        // v1 returns plain array
        ResponseEntity<BookResponse[]> v1 = restTemplate.getForEntity(
                "/api/v1/books", BookResponse[].class);
        assertThat(v1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(v1.getBody()).hasSize(1);
        // v1 response has authorName (not a wrapper)
        assertThat(v1.getBody()[0].getAuthorName()).isEqualTo("Grady Booch");

        // v2 returns wrapper
        ResponseEntity<PagedResponseV2> v2 = restTemplate.getForEntity(
                "/api/v2/books", PagedResponseV2.class);
        assertThat(v2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(v2.getBody().getVersion()).isEqualTo("v2");
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
