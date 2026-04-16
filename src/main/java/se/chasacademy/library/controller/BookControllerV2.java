package se.chasacademy.library.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.chasacademy.library.dto.response.BookResponseV2;
import se.chasacademy.library.dto.response.PagedResponseV2;
import se.chasacademy.library.service.BookService;

import java.util.List;

/**
 * REST Controller for Library API — Version 2.
 *
 * Differences from v1:
 *  - Response includes 'available' boolean field
 *  - Response is wrapped in a {@link PagedResponseV2} envelope with a "version" field
 *
 * v1 endpoints are completely unaffected by this class.
 */
@RestController
@RequestMapping("/api/v2/books")
@Tag(name = "Books v2", description = "Library API version 2 — enriched responses with availability and envelope wrapper")
public class BookControllerV2 {

    private final BookService bookService;

    public BookControllerV2(BookService bookService) {
        this.bookService = bookService;
    }

    // ── GET /api/v2/books ─────────────────────────────────────────────────────

    @Operation(
            summary = "Get all books (v2)",
            description = "Returns all books wrapped in a versioned envelope. Each book includes the 'available' field not present in v1."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Books returned in v2 wrapper format",
                    content = @Content(schema = @Schema(implementation = PagedResponseV2.class)))
    })
    @GetMapping
    public ResponseEntity<PagedResponseV2<BookResponseV2>> getAllBooks() {
        List<BookResponseV2> books = bookService.getAllBooksV2();
        PagedResponseV2<BookResponseV2> response = new PagedResponseV2<>(books, "v2");
        return ResponseEntity.ok(response);
    }
}
