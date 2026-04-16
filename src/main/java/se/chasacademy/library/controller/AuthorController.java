package se.chasacademy.library.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.chasacademy.library.dto.request.AuthorRequest;
import se.chasacademy.library.dto.response.AuthorResponse;
import se.chasacademy.library.dto.response.BookResponse;
import se.chasacademy.library.exception.ApiError;
import se.chasacademy.library.service.AuthorService;

import java.util.List;

/**
 * REST Controller for Author management.
 * Delegates all logic to {@link AuthorService} — no business logic here.
 */
@RestController
@RequestMapping("/api/v1/authors")
@Tag(name = "Authors v1", description = "Author management — create and retrieve authors and their books")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    // ── POST /api/v1/authors ──────────────────────────────────────────────────

    @Operation(summary = "Create a new author",
            description = "Creates a new author. Name must not be blank. Returns 201 with the created author.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Author created successfully",
                    content = @Content(schema = @Schema(implementation = AuthorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed — name is blank",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<AuthorResponse> createAuthor(@Valid @RequestBody AuthorRequest request) {
        AuthorResponse response = authorService.createAuthor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── GET /api/v1/authors/{id} ──────────────────────────────────────────────

    @Operation(summary = "Get an author by ID",
            description = "Returns a single author by ID. Returns 404 if the author does not exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Author found",
                    content = @Content(schema = @Schema(implementation = AuthorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Author not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AuthorResponse> getAuthorById(@PathVariable Long id) {
        AuthorResponse response = authorService.getAuthorById(id);
        return ResponseEntity.ok(response);
    }

    // ── GET /api/v1/authors/{id}/books ────────────────────────────────────────

    @Operation(summary = "Get all books by an author",
            description = "Returns a list of all books belonging to the specified author. Returns 404 if the author does not exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Books returned successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Author not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}/books")
    public ResponseEntity<List<BookResponse>> getBooksByAuthor(@PathVariable Long id) {
        List<BookResponse> books = authorService.getBooksByAuthorId(id);
        return ResponseEntity.ok(books);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuthorResponse> updateAuthor(@PathVariable Long id, @Valid @RequestBody AuthorRequest request) {
        AuthorResponse response = authorService.updateAuthor(id, request);
        return ResponseEntity.ok(response);
    }
}



// example curl commands:
// Create an author:
// curl -X POST http://localhost:8080/api/v1/authors \
//      -H "Content-Type: application/json" \
//      -d '{"name": "J.K. Rowling"}'
// Get an author by ID:
// curl http://localhost:8080/api/v1/authors/1
// Get all books by an author:
// curl http://localhost:8080/api/v1/authors/1/books
//! example JSON request body for POST /api/v1/authors
//!{
//!  "name": "J.K. Rowling"
//!}
//!
//! example JSON response for GET /api/v1/authors/1
//!{
//!  "id": 1,
//!  "name": "J.K. Rowling"
//!}
//!! example JSON response for GET /api/v1/authors/1/books
//![!  {
//!    "id": 1,
//!    "title": "Harry Potter and the Sorcerer's Stone",
//!    "publicationYear": 1997,
//!    "author": {
//!      "id": 1,
//!      "name": "J.K. Rowling"
//!    }
//!  },
//!  {
//!    "id": 2,
//!    "title": "Harry Potter and the Chamber of Secrets",
//!    "publicationYear": 1998,
//!    "author": {
//!      "id": 1,
//!      "name": "J.K. Rowling"
//!    }
//!  }
//!]
