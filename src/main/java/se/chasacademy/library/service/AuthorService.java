package se.chasacademy.library.service;

import org.springframework.stereotype.Service;
import se.chasacademy.library.dto.request.AuthorRequest;
import se.chasacademy.library.dto.response.AuthorResponse;
import se.chasacademy.library.dto.response.BookResponse;
import se.chasacademy.library.entity.Author;
import se.chasacademy.library.exception.AuthorNotFoundException;
import se.chasacademy.library.repository.AuthorRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for all Author business logic.
 * Controllers ONLY call this class — never the Repository directly.
 */
@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    /**
     * Creates and persists a new Author.
     */
    public AuthorResponse createAuthor(AuthorRequest request) {
        Author author = new Author(request.getName());
        Author saved = authorRepository.save(author);
        return toResponse(saved);
    }

    /**
     * Retrieves a single Author by ID.
     *
     * @throws AuthorNotFoundException if not found
     */
    public AuthorResponse getAuthorById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));
        return toResponse(author);
    }

    /**
     * Retrieves all books belonging to a specific author.
     *
     * @throws AuthorNotFoundException if the author doesn't exist
     */
    public List<BookResponse> getBooksByAuthorId(Long authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new AuthorNotFoundException(authorId));

        return author.getBooks()
                .stream()
                .map(book -> new BookResponse(
                        book.getId(),
                        book.getTitle(),
                        author.getName(),
                        author.getId()
                ))
                .toList();
    }


//     uppdateAuthor() and deleteAuthor()
    public AuthorResponse updateAuthor(Long id, AuthorRequest request) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));

        author.setName(request.getName());
        Author updated = authorRepository.save(author);
        return toResponse(updated);
    }

    public void deleteAuthor(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));
        authorRepository.delete(author);
    }





    // ── Private Mapping Helpers ───────────────────────────────────────────────

    private AuthorResponse toResponse(Author author) {
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                author.getBooks().size()
        );
    }
}
