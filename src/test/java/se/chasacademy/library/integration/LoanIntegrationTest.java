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
import se.chasacademy.library.dto.request.LoanRequest;
import se.chasacademy.library.dto.response.AuthorResponse;
import se.chasacademy.library.dto.response.BookResponse;
import se.chasacademy.library.dto.response.LoanResponse;
import se.chasacademy.library.exception.ApiError;
import se.chasacademy.library.repository.AuthorRepository;
import se.chasacademy.library.repository.BookRepository;
import se.chasacademy.library.repository.LoanRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Loan endpoints — Övning 1 & 2.
 *
 * Covers:
 *  - POST /api/v1/loans  (201 + 404 book-not-found + 400 already-on-loan)
 *  - GET  /api/v1/loans  (with data + empty list)
 *
 * Rules:
 *  - Full HTTP stack — no mocks
 *  - @BeforeEach clears all tables
 *  - Tests are completely independent
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoanIntegrationTest {

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

    // ── POST /api/v1/loans — Happy Path ───────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/loans — 201 Created with correct loan data")
    void createLoan_shouldReturn201WithLoanData() {
        Long authorId = createAuthor("Erich Gamma");
        Long bookId = createBook("Design Patterns", authorId);

        ResponseEntity<LoanResponse> response = restTemplate.postForEntity(
                "/api/v1/loans", new LoanRequest(bookId), LoanResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LoanResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull().isPositive();
        assertThat(body.getBookId()).isEqualTo(bookId);
        assertThat(body.getBookTitle()).isEqualTo("Design Patterns");
        assertThat(body.getAuthorName()).isEqualTo("Erich Gamma");
        assertThat(body.getLoanDate()).isEqualTo(LocalDate.now()); // auto-set today
        assertThat(body.getReturnDate()).isNull(); // not returned yet
        assertThat(loanRepository.count()).isEqualTo(1);
    }

    // ── POST /api/v1/loans — Book Not Found → 404 ────────────────────────────

    @Test
    @DisplayName("POST /api/v1/loans — 404 when bookId does not exist")
    void createLoan_bookNotFound_shouldReturn404() {
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/v1/loans", new LoanRequest(99999L), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiError error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getError()).isEqualTo("Not Found");
        assertThat(error.getMessage()).contains("99999");
        assertThat(error.getPath()).isEqualTo("/api/v1/loans");
        // No loan should be created
        assertThat(loanRepository.count()).isZero();
    }

    // ── POST /api/v1/loans — Already On Loan → 400 ───────────────────────────

    @Test
    @DisplayName("POST /api/v1/loans — 400 when book is already on loan")
    void createLoan_alreadyOnLoan_shouldReturn400() {
        Long authorId = createAuthor("Fred Brooks");
        Long bookId = createBook("The Mythical Man-Month", authorId);

        // First loan — should succeed
        ResponseEntity<LoanResponse> first = restTemplate.postForEntity(
                "/api/v1/loans", new LoanRequest(bookId), LoanResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Second loan on the same book — should fail
        ResponseEntity<ApiError> second = restTemplate.postForEntity(
                "/api/v1/loans", new LoanRequest(bookId), ApiError.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError error = second.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getMessage()).isEqualTo("Book is already on loan");
        // Still only one loan in DB
        assertThat(loanRepository.count()).isEqualTo(1);
    }

    // ── POST /api/v1/loans — Null bookId → 400 ───────────────────────────────

    @Test
    @DisplayName("POST /api/v1/loans — 400 when bookId is null")
    void createLoan_nullBookId_shouldReturn400() {
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/v1/loans", new LoanRequest(null), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).containsIgnoringCase("book");
    }

    // ── GET /api/v1/loans — With Active Loans ────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/loans — 200 with list of all active loans")
    void getAllLoans_shouldReturn200WithLoans() {
        Long authorId = createAuthor("Andy Hunt");
        Long book1 = createBook("The Pragmatic Programmer", authorId);
        Long book2 = createBook("Programming Ruby", authorId);

        // Loan both books
        restTemplate.postForEntity("/api/v1/loans", new LoanRequest(book1), LoanResponse.class);
        restTemplate.postForEntity("/api/v1/loans", new LoanRequest(book2), LoanResponse.class);

        ResponseEntity<LoanResponse[]> response = restTemplate.getForEntity(
                "/api/v1/loans", LoanResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoanResponse[] loans = response.getBody();
        assertThat(loans).isNotNull().hasSize(2);
        assertThat(loans).extracting(LoanResponse::getAuthorName)
                .containsOnly("Andy Hunt");
        assertThat(loans).extracting(LoanResponse::getLoanDate)
                .containsOnly(LocalDate.now());
        assertThat(loans).extracting(LoanResponse::getReturnDate)
                .containsOnlyNulls(); // all still on loan
    }

    // ── GET /api/v1/loans — Empty List (Edge Case) ───────────────────────────

    @Test
    @DisplayName("GET /api/v1/loans — 200 with empty array when no loans exist")
    void getAllLoans_empty_shouldReturn200EmptyList() {
        ResponseEntity<LoanResponse[]> response = restTemplate.getForEntity(
                "/api/v1/loans", LoanResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    // ── Loan data persists correctly across requests ──────────────────────────

    @Test
    @DisplayName("POST /api/v1/loans then GET — loan appears in list with correct fields")
    void createLoan_thenGetAll_shouldContainLoan() {
        Long authorId = createAuthor("Gene Kim");
        Long bookId = createBook("The Phoenix Project", authorId);

        ResponseEntity<LoanResponse> created = restTemplate.postForEntity(
                "/api/v1/loans", new LoanRequest(bookId), LoanResponse.class);

        Long loanId = created.getBody().getId();

        ResponseEntity<LoanResponse[]> all = restTemplate.getForEntity(
                "/api/v1/loans", LoanResponse[].class);

        assertThat(all.getBody()).hasSize(1);
        LoanResponse found = all.getBody()[0];
        assertThat(found.getId()).isEqualTo(loanId);
        assertThat(found.getBookTitle()).isEqualTo("The Phoenix Project");
        assertThat(found.getReturnDate()).isNull();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Long createAuthor(String name) {
        ResponseEntity<AuthorResponse> r = restTemplate.postForEntity(
                "/api/v1/authors", new AuthorRequest(name), AuthorResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().getId();
    }

    private Long createBook(String title, Long authorId) {
        ResponseEntity<BookResponse> r = restTemplate.postForEntity(
                "/api/v1/books", new BookRequest(title, authorId), BookResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().getId();
    }
}
