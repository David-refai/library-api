package se.chasacademy.library.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import se.chasacademy.library.dto.request.BookRequest;
import se.chasacademy.library.dto.response.BookResponse;
import se.chasacademy.library.exception.ApiError;
import se.chasacademy.library.repository.BookRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for POST /api/v1/books and GET /api/v1/books/{id}.
 *
 * Rules:
 * - @SpringBootTest with RANDOM_PORT starts the full application context
 * - TestRestTemplate hits the real HTTP stack — no mocks
 * - Real H2 in-memory database is used (see test/resources/application.properties)
 * - Tests are independent — @BeforeEach resets the database state
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookRepository bookRepository;

    /**
     * Ensures each test starts with a clean database — no residual data
     * from previous tests can affect results.
     */
    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    // ── Test 1: POST /api/v1/books ────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/books — should persist book and return 201 Created")
    void createBook_shouldReturn201AndPersistBook() {
        // Arrange
        BookRequest request = new BookRequest("Clean Code", "Robert C. Martin");

        // Act
        ResponseEntity<BookResponse> response = restTemplate.postForEntity(
                "/api/v1/books",
                request,
                BookResponse.class
        );

        // Assert — HTTP status
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Assert — response body
        BookResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Clean Code");
        assertThat(body.getAuthor()).isEqualTo("Robert C. Martin");

        // Assert — actually persisted in database
        assertThat(bookRepository.count()).isEqualTo(1);
        assertThat(bookRepository.findById(body.getId())).isPresent();
    }

    // ── Test 2: GET /api/v1/books/{id} — book exists ─────────────────────────

    @Test
    @DisplayName("GET /api/v1/books/{id} — should return 200 and the correct book")
    void getBookById_shouldReturn200WithCorrectBook() {
        // Arrange — first create a book via POST
        BookRequest createRequest = new BookRequest("The Pragmatic Programmer", "Andrew Hunt");
        ResponseEntity<BookResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/books",
                createRequest,
                BookResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long createdId = createResponse.getBody().getId();

        // Act — fetch the created book by ID
        ResponseEntity<BookResponse> getResponse = restTemplate.getForEntity(
                "/api/v1/books/" + createdId,
                BookResponse.class
        );

        // Assert
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        BookResponse body = getResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(createdId);
        assertThat(body.getTitle()).isEqualTo("The Pragmatic Programmer");
        assertThat(body.getAuthor()).isEqualTo("Andrew Hunt");
    }

    // ── Test 3 (Bonus): GET /api/v1/books/{id} — book does NOT exist ─────────

    @Test
    @DisplayName("GET /api/v1/books/{id} — should return 404 when book does not exist")
    void getBookById_nonExistentId_shouldReturn404() {
        // Act — request a book that does not exist
        ResponseEntity<ApiError> response = restTemplate.getForEntity(
                "/api/v1/books/99999",
                ApiError.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ApiError error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getError()).isEqualTo("Not Found");
        assertThat(error.getMessage()).contains("99999");
        assertThat(error.getPath()).isEqualTo("/api/v1/books/99999");
        assertThat(error.getTimestamp()).isNotNull();
    }

    // ── Test 4 (Bonus): POST validation — blank title ─────────────────────────

    @Test
    @DisplayName("POST /api/v1/books — should return 400 when title is blank")
    void createBook_blankTitle_shouldReturn400() {
        // Arrange — request with blank title
        BookRequest invalidRequest = new BookRequest("", "Some Author");

        // Act
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/v1/books",
                invalidRequest,
                ApiError.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ApiError error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getMessage()).containsIgnoringCase("title");
    }
}
