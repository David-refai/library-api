package se.chasacademy.library.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.chasacademy.library.entity.Author;
import se.chasacademy.library.entity.Book;
import se.chasacademy.library.entity.Loan;
import se.chasacademy.library.repository.AuthorRepository;
import se.chasacademy.library.repository.BookRepository;
import se.chasacademy.library.repository.LoanRepository;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(AuthorRepository authorRepo, 
                                   BookRepository bookRepo, 
                                   LoanRepository loanRepo) {
        return args -> {
            if (authorRepo.count() > 0) return; // Prevent duplicate seeding

            // 1. Create Authors
            Author martin = authorRepo.save(new Author("Robert C. Martin"));
            Author fowler = authorRepo.save(new Author("Martin Fowler"));
            Author bloch = authorRepo.save(new Author("Joshua Bloch"));
            Author gamma = authorRepo.save(new Author("Erich Gamma"));

            // 2. Create Books
            Book cleanCode = bookRepo.save(new Book("Clean Code", martin));
            Book cleanArch = bookRepo.save(new Book("Clean Architecture", martin));
            Book refactoring = bookRepo.save(new Book("Refactoring", fowler));
            Book effJava = bookRepo.save(new Book("Effective Java", bloch));
            Book designPatterns = bookRepo.save(new Book("Design Patterns", gamma));
            Author evans = authorRepo.save(new Author("Eric Evans"));
            bookRepo.save(new Book("Domain-Driven Design", evans));

            // 3. Create some Loans
            cleanCode.setAvailable(false);
            bookRepo.save(cleanCode);
            Loan loan1 = new Loan(cleanCode);
            loan1.setLoanDate(LocalDate.now().minusDays(10));
            loanRepo.save(loan1);

            refactoring.setAvailable(false);
            bookRepo.save(refactoring);
            Loan loan2 = new Loan(refactoring);
            loan2.setLoanDate(LocalDate.now().minusDays(5));
            loanRepo.save(loan2);
            
            System.out.println("✅ Database Seeded: 5 Authors, 6 Books, 2 Loans created.");
        };
    }
}
