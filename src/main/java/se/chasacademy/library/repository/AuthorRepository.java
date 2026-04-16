package se.chasacademy.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.chasacademy.library.entity.Author;

/**
 * Spring Data JPA Repository for Author entities.
 * Only accessed from AuthorService — never from Controllers.
 */
@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
}
