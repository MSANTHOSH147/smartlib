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
}