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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Author and Loan endpoints (Vecka 3 – Övning 5 & 6).
 *
 * Rules:
 * - @SpringBootTest with RANDOM_PORT — full application context, no mocks
 * - TestRestTemplate for all HTTP calls
 * - Real H2 in-memory database
 * - @BeforeEach clears all tables — tests are fully independent
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthorLoanIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    /**
     * Clear all data before each test in FK-safe order.
     */
    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }

    // ── Test 1: Create Author + Book → GET /authors/{id}/books ───────────────

    @Test
    @DisplayName("Test 1 – Create Author + Book, then GET /authors/{id}/books returns the book")
    void createAuthorAndBook_thenGetBooksByAuthor_shouldReturnBook() {
        // Step 1: Create Author
        ResponseEntity<AuthorResponse> authorResponse = restTemplate.postForEntity(
                "/api/v1/authors",
                new AuthorRequest("Martin Fowler"),
                AuthorResponse.class
        );
        assertThat(authorResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long authorId = authorResponse.getBody().getId();
        assertThat(authorResponse.getBody().getName()).isEqualTo("Martin Fowler");

        // Step 2: Create Book linked to the Author
        ResponseEntity<BookResponse> bookResponse = restTemplate.postForEntity(
                "/api/v1/books",
                new BookRequest("Refactoring", authorId),
                BookResponse.class
        );
        assertThat(bookResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bookResponse.getBody().getAuthorName()).isEqualTo("Martin Fowler");

        // Step 3: GET /authors/{id}/books
        ResponseEntity<BookResponse[]> booksResponse = restTemplate.getForEntity(
                "/api/v1/authors/" + authorId + "/books",
                BookResponse[].class
        );
        assertThat(booksResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        BookResponse[] books = booksResponse.getBody();
        assertThat(books).hasSize(1);
        assertThat(books[0].getTitle()).isEqualTo("Refactoring");
        assertThat(books[0].getAuthorName()).isEqualTo("Martin Fowler");
    }

    // ── Test 2: Create Loan → 201 + GET /loans returns the loan ──────────────

    @Test
    @DisplayName("Test 2 – Create Loan then GET /loans returns the active loan")
    void createLoan_thenGetAllLoans_shouldReturnLoan() {
        // Step 1: Setup — Author + Book
        Long authorId = createAuthor("Joshua Bloch");
        Long bookId = createBook("Effective Java", authorId);

        // Step 2: Create Loan
        ResponseEntity<LoanResponse> loanResponse = restTemplate.postForEntity(
                "/api/v1/loans",
                new LoanRequest(bookId),
                LoanResponse.class
        );
        assertThat(loanResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LoanResponse loan = loanResponse.getBody();
        assertThat(loan).isNotNull();
        assertThat(loan.getBookId()).isEqualTo(bookId);
        assertThat(loan.getBookTitle()).isEqualTo("Effective Java");
        assertThat(loan.getLoanDate()).isNotNull();
        assertThat(loan.getReturnDate()).isNull(); // still on loan

        // Step 3: GET /loans returns it
        ResponseEntity<LoanResponse[]> allLoans = restTemplate.getForEntity(
                "/api/v1/loans", LoanResponse[].class);
        assertThat(allLoans.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allLoans.getBody()).hasSize(1);
        assertThat(allLoans.getBody()[0].getBookTitle()).isEqualTo("Effective Java");
    }

    // ── Test 3: Loan already on loan → 400 Bad Request ───────────────────────

    @Test
    @DisplayName("Test 3 – Attempting to loan an already-loaned book returns 400")
    void createLoan_alreadyOnLoan_shouldReturn400() {
        // Step 1: Setup — Author + Book + First Loan
        Long authorId = createAuthor("Donald Knuth");
        Long bookId = createBook("The Art of Computer Programming", authorId);

        ResponseEntity<LoanResponse> firstLoan = restTemplate.postForEntity(
                "/api/v1/loans", new LoanRequest(bookId), LoanResponse.class);
        assertThat(firstLoan.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Step 2: Attempt second loan on the same book
        ResponseEntity<ApiError> secondLoan = restTemplate.postForEntity(
                "/api/v1/loans", new LoanRequest(bookId), ApiError.class);

        // Assert — 400 with correct message
        assertThat(secondLoan.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError error = secondLoan.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getMessage()).isEqualTo("Book is already on loan");
    }

    // ── Test 4 (Övning 6): Race Condition — two threads, one book ────────────

    @Test
    @DisplayName("Test 4 (Övning 6) – Concurrent loan attempts: exactly one succeeds")
    void createLoan_concurrentRequests_exactlyOneSucceeds() throws InterruptedException {
        // Setup
        Long authorId = createAuthor("Edsger Dijkstra");
        Long bookId = createBook("A Discipline of Programming", authorId);

        int threadCount = 2;
        /*
         * CountDownLatch makes both threads start at exactly the same moment,
         * maximizing the chance of hitting the race condition.
         *
         * Two possible outcomes per thread:
         *  - 201 Created  → @Transactional service check caught it first (happy path)
         *  - 400 Bad Request → BookAlreadyOnLoanException from service
         *  - 409 Conflict    → DB unique constraint fired (DataIntegrityViolationException)
         *
         * Either way: exactly one loan is created.
         */
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // hold until all threads are ready
                    ResponseEntity<String> response = restTemplate.postForEntity(
                            "/api/v1/loans", new LoanRequest(bookId), String.class);

                    int statusCode = response.getStatusCode().value();
                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 400 || statusCode == 409) {
                        // 400 = service caught the duplicate, 409 = DB constraint fired
                        rejectedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // release all threads simultaneously
        doneLatch.await();      // wait for both to finish
        executor.shutdown();

        // Assert: exactly 1 loan created, 1 rejected — regardless of mechanism
        assertThat(successCount.get())
                .as("Exactly one loan should succeed")
                .isEqualTo(1);
        assertThat(rejectedCount.get())
                .as("Exactly one loan should be rejected (400 or 409)")
                .isEqualTo(1);
        assertThat(loanRepository.count())
                .as("Database should contain exactly one loan")
                .isEqualTo(1);
    }

    // ── Private Helper Methods ────────────────────────────────────────────────

    private Long createAuthor(String name) {
        ResponseEntity<AuthorResponse> response = restTemplate.postForEntity(
                "/api/v1/authors", new AuthorRequest(name), AuthorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().getId();
    }

    private Long createBook(String title, Long authorId) {
        ResponseEntity<BookResponse> response = restTemplate.postForEntity(
                "/api/v1/books", new BookRequest(title, authorId), BookResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().getId();
    }
}
