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
import se.chasacademy.library.dto.request.LoanRequest;
import se.chasacademy.library.dto.response.LoanResponse;
import se.chasacademy.library.exception.ApiError;
import se.chasacademy.library.service.LoanService;

import java.util.List;

/**
 * REST Controller for Loan management.
 * Delegates all logic to {@link LoanService} — no business logic here.
 */
@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loans v1", description = "Loan management — borrow and list books on loan")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // ── POST /api/v1/loans ────────────────────────────────────────────────────

    @Operation(summary = "Create a new loan",
            description = "Loans a book by its ID. Returns 201 if successful. Returns 400 if the book is already on loan. Returns 404 if the book does not exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan created successfully",
                    content = @Content(schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Book is already on loan",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Book not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<LoanResponse> createLoan(@Valid @RequestBody LoanRequest request) {
        LoanResponse response = loanService.createLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── GET /api/v1/loans ─────────────────────────────────────────────────────

    @Operation(summary = "Get all active loans",
            description = "Returns a list of all current loans. Empty list if no loans exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loans returned successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LoanResponse.class))))
    })
    @GetMapping
    public ResponseEntity<List<LoanResponse>> getAllLoans() {
        List<LoanResponse> loans = loanService.getAllLoans();
        return ResponseEntity.ok(loans);
    }
}
