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
import se.chasacademy.library.dto.request.BookRequest;
import se.chasacademy.library.dto.response.BookResponse;
import se.chasacademy.library.exception.ApiError;
import se.chasacademy.library.service.BookService;

import java.util.List;

/**
 * REST Controller for Library API — Version 1.
 *
 * Responsibilities:
 *  - Accept HTTP requests and delegate to {@link BookService}
 *  - Return appropriate HTTP status codes and DTO responses
 *  - NO business logic lives here
 *  - Entities are NEVER returned directly — only DTOs
 */
@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books v1", description = "Library API version 1 — core book management endpoints")
public class BookControllerV1 {

    private final BookService bookService;

    public BookControllerV1(BookService bookService) {
        this.bookService = bookService;
    }

    // ── POST /api/v1/books ────────────────────────────────────────────────────

    @Operation(
            summary = "Create a new book",
            description = "Creates a new book entry. Title and author must not be blank. Returns 201 with the created book."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book created successfully",
                    content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed — title or author is blank",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        BookResponse response = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── GET /api/v1/books ─────────────────────────────────────────────────────

    @Operation(
            summary = "Get all books",
            description = "Returns a list of all books in the library. Empty list if no books exist."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of books returned successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookResponse.class))))
    })
    @GetMapping
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        List<BookResponse> books = bookService.getAllBooks();
        return ResponseEntity.ok(books);
    }

    // ── GET /api/v1/books/{id} ────────────────────────────────────────────────

    @Operation(
            summary = "Get a book by ID",
            description = "Returns a single book by its numeric ID. Returns 404 if the book does not exist."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book found and returned",
                    content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        BookResponse response = bookService.getBookById(id);
        return ResponseEntity.ok(response);
    }
}
