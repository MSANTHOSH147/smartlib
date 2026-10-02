package com.smartlib.repository;

import com.smartlib.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByTitleContainingIgnoreCase(String title);

    List<Book> findByAuthorContainingIgnoreCase(String author);

    List<Book> findByCategoryId(Long categoryId);

    List<Book> findByAvailableCopiesGreaterThan(Integer copies);

    boolean existsByIsbn(String isbn);

    @org.springframework.data.jpa.repository.Query("""
        SELECT b FROM Book b
        WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(b.publisher) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(b.description) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(b.category.name) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    List<Book> searchLexical(@org.springframework.data.repository.query.Param("query") String query);
}