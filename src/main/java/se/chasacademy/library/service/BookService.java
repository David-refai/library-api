package se.chasacademy.library.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import se.chasacademy.library.dto.request.BookRequest;
import se.chasacademy.library.dto.response.BookResponse;
import se.chasacademy.library.dto.response.BookResponseV2;
import se.chasacademy.library.entity.Author;
import se.chasacademy.library.entity.Book;
import se.chasacademy.library.exception.AuthorNotFoundException;
import se.chasacademy.library.exception.BookNotFoundException;
import se.chasacademy.library.repository.AuthorRepository;
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
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    /**
     * Creates and persists a new Book.
     * Looks up the Author by authorId — throws AuthorNotFoundException if not found.
     */
    @CacheEvict(value = {"books", "booksV2"}, allEntries = true)
    public BookResponse createBook(BookRequest request) {
        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new AuthorNotFoundException(request.getAuthorId()));

        Book book = new Book(request.getTitle(), author);
        Book saved = bookRepository.save(book);
        return toResponse(saved);
    }

    /**
     * Retrieves all books from the database.
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
     * @throws BookNotFoundException if no book with the given ID exists
     */
    @Cacheable(value = "books", key = "#id")
    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        return toResponse(book);
    }

    /**
     * Retrieves all books as v2 DTOs (includes 'available' field).
     */
    @Cacheable(value = "booksV2", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<BookResponseV2> getAllBooksV2(Pageable pageable) {
        return bookRepository.findAll(pageable)
                .map(this::toResponseV2);
    }

    /**
     * Simulates an external API call to fetch book metadata.
     * Protected by Resilience4j Circuit Breaker.
     */
    @CircuitBreaker(name = "bookMetadataService", fallbackMethod = "fallbackForMetadata")
    public String getBookMetadata(Long id) {
        // Simulate a network failure 30% of the time
        if (Math.random() < 0.3) {
            throw new RuntimeException("External Metadata Service is down");
        }
        return "Comprehensive metadata for book ID " + id + " fetched from external source.";
    }

    public String fallbackForMetadata(Long id, Throwable t) {
        return "Metadata temporarily unavailable. Please try again later. (Reason: " + t.getMessage() + ")";
    }



    // ── Private Mapping Helpers ───────────────────────────────────────────────

    private BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor().getName(),
                book.getAuthor().getId()
        );
    }

    private BookResponseV2 toResponseV2(Book book) {
        return new BookResponseV2(
                book.getId(),
                book.getTitle(),
                book.getAuthor().getName(),
                book.getAuthor().getId(),
                book.isAvailable()
        );
    }
}
