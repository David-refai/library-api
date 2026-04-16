package se.chasacademy.library.service;

import org.springframework.stereotype.Service;
import se.chasacademy.library.dto.request.BookRequest;
import se.chasacademy.library.dto.response.BookResponse;
import se.chasacademy.library.dto.response.BookResponseV2;
import se.chasacademy.library.entity.Book;
import se.chasacademy.library.exception.BookNotFoundException;
import se.chasacademy.library.repository.BookRepository;

import java.util.List;

/**
 * Service layer for all Book business logic.
 * Controllers ONLY call this class — never the Repository directly.
 * All entity-to-DTO mapping happens here.
 */
@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Creates and persists a new Book from the given request DTO.
     *
     * @param request the BookRequest DTO received from the controller
     * @return a BookResponse DTO representing the saved book
     */
    public BookResponse createBook(BookRequest request) {
        Book book = new Book(request.getTitle(), request.getAuthor());
        Book saved = bookRepository.save(book);
        return toResponse(saved);
    }

    /**
     * Retrieves all books from the database.
     *
     * @return list of BookResponse DTOs
     */
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves a single book by ID.
     *
     * @param id the ID of the book
     * @return a BookResponse DTO
     * @throws BookNotFoundException if no book with the given ID exists
     */
    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        return toResponse(book);
    }

    /**
     * Retrieves all books as v2 DTOs (includes 'available' field).
     *
     * @return list of BookResponseV2 DTOs
     */
    public List<BookResponseV2> getAllBooksV2() {
        return bookRepository.findAll()
                .stream()
                .map(this::toResponseV2)
                .toList();
    }

    // ── Private Mapping Helpers ───────────────────────────────────────────────

    private BookResponse toResponse(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor());
    }

    private BookResponseV2 toResponseV2(Book book) {
        return new BookResponseV2(book.getId(), book.getTitle(), book.getAuthor(), book.isAvailable());
    }
}
