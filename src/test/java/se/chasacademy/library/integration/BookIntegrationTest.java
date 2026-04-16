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
import se.chasacademy.library.repository.BookRepository;
import se.chasacademy.library.repository.LoanRepository;
import se.chasacademy.library.repository.AuthorRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Updated integration tests for Book endpoints.
 * Since Book now requires an Author, @BeforeEach creates an Author first.
 *
 * Rules:
 * - @SpringBootTest with RANDOM_PORT — full application context
 * - TestRestTemplate — real HTTP, no mocks
 * - Real H2 in-memory database
 * - Tests are independent — @BeforeEach resets all tables
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AuthorRepository authorRepository;

    /** ID of a pre-created Author, shared across tests in this class. */
    private Long authorId;

    /**
     * Clean all tables and create a shared Author before each test.
     * Order: Loans → Books → Authors (FK dependency order).
     */
    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();

        // Create a reusable author for book tests
        ResponseEntity<AuthorResponse> authorResponse = restTemplate.postForEntity(
                "/api/v1/authors",
                new AuthorRequest("Robert C. Martin"),
                AuthorResponse.class
        );
        assertThat(authorResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        this.authorId = authorResponse.getBody().getId();
    }

    // ── Test 1: POST /api/v1/books ────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/books — should persist book and return 201 Created")
    void createBook_shouldReturn201AndPersistBook() {
        // Arrange
        BookRequest request = new BookRequest("Clean Code", authorId);

        // Act
        ResponseEntity<BookResponse> response = restTemplate.postForEntity(
                "/api/v1/books", request, BookResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BookResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Clean Code");
        assertThat(body.getAuthorName()).isEqualTo("Robert C. Martin");
        assertThat(body.getAuthorId()).isEqualTo(authorId);
        assertThat(bookRepository.count()).isEqualTo(1);
    }

    // ── Test 2: GET /api/v1/books/{id} — book exists ─────────────────────────

    @Test
    @DisplayName("GET /api/v1/books/{id} — should return 200 and the correct book")
    void getBookById_shouldReturn200WithCorrectBook() {
        // Arrange — create book
        ResponseEntity<BookResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/books", new BookRequest("The Pragmatic Programmer", authorId), BookResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long createdId = createResponse.getBody().getId();

        // Act
        ResponseEntity<BookResponse> getResponse = restTemplate.getForEntity(
                "/api/v1/books/" + createdId, BookResponse.class);

        // Assert
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        BookResponse body = getResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(createdId);
        assertThat(body.getTitle()).isEqualTo("The Pragmatic Programmer");
        assertThat(body.getAuthorName()).isEqualTo("Robert C. Martin");
    }

    // ── Test 3: GET non-existent book ─────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/books/{id} — should return 404 when book does not exist")
    void getBookById_nonExistentId_shouldReturn404() {
        ResponseEntity<ApiError> response = restTemplate.getForEntity(
                "/api/v1/books/99999", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiError error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getMessage()).contains("99999");
    }

    // ── Test 4: POST with blank title ─────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/books — should return 400 when title is blank")
    void createBook_blankTitle_shouldReturn400() {
        BookRequest invalidRequest = new BookRequest("", authorId);

        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/v1/books", invalidRequest, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).containsIgnoringCase("title");
    }
}
