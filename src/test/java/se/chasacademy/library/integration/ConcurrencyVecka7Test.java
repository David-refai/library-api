package se.chasacademy.library.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import se.chasacademy.library.dto.request.AuthorRequest;
import se.chasacademy.library.dto.request.BookRequest;
import se.chasacademy.library.dto.request.LoanRequest;
import se.chasacademy.library.dto.response.AuthorResponse;
import se.chasacademy.library.dto.response.BookResponse;
import se.chasacademy.library.repository.AuthorRepository;
import se.chasacademy.library.repository.BookRepository;
import se.chasacademy.library.repository.LoanRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vecka 7 – Concurrency (Mål: 100 samtidiga anrop)
 * 
 * Testar hur systemet hanterar 100 parallella anrop till POST /api/v1/loans för samma bok.
 * Om vi inte har Thead-Safety (tex @Version Optimistic Locking) sker Data Corruption (flera lån skapas).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrencyVecka7Test {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private LoanRepository loanRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private AuthorRepository authorRepository;

    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    @DisplayName("Vecka 7 – 100 samtidiga lån ska resultera i exakt ETT godkänt lån (Ingen Data Corruption)")
    void concurrentLoans_100requests_shouldResultInExactlyOneLoan() throws InterruptedException {
        // Skapa en bok med en författare
        Long authorId = createAuthor("Edsger W. Dijkstra");
        Long bookId = createBook("A Discipline of Programming", authorId);

        int threadCount = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Håll tråden tills ALLA 100 är redo

                    ResponseEntity<String> response = restTemplate.postForEntity(
                            "/api/v1/loans", new LoanRequest(bookId), String.class);

                    int statusCode = response.getStatusCode().value();
                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 400 || statusCode == 409) {
                        rejectedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Släpp alla 100 anrop exakt samtidigt!
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // ── Validering mot datakorruption ──
        assertThat(successCount.get())
                .as("Exakt 1 lån (1 vinnare) måste lyckas")
                .isEqualTo(1);
        assertThat(rejectedCount.get())
                .as("99 anrop (förlorare) måste få Bad Request eller Conflict")
                .isEqualTo(99);
                
        // Verifiera i databasen att bara ett lån skapats för boken
        long loansInDb = loanRepository.count();
        assertThat(loansInDb)
                .as("Databasen ska bara ha ETT lån totalt, ingen datakorruption")
                .isEqualTo(1L);
    }

    // ── Hjälpmetoder ──────────────────────────────────────────────────────────

    private Long createAuthor(String name) {
        ResponseEntity<AuthorResponse> r = restTemplate.postForEntity(
                "/api/v1/authors", new AuthorRequest(name), AuthorResponse.class);
        return r.getBody().getId();
    }

    private Long createBook(String title, Long authorId) {
        ResponseEntity<BookResponse> r = restTemplate.postForEntity(
                "/api/v1/books", new BookRequest(title, authorId), BookResponse.class);
        return r.getBody().getId();
    }
}
