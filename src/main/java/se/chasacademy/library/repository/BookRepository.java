package se.chasacademy.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.chasacademy.library.entity.Book;

/**
 * Spring Data JPA Repository for Book entities.
 * Provides CRUD operations out of the box.
 * Only accessed from the Service layer — never from Controllers.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
}
