package com.smartlib.controller;

import com.smartlib.dto.book.BookRequest;
import com.smartlib.dto.book.BookResponse;
import com.smartlib.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<List<BookResponse>> getAllBooks() {

        return ResponseEntity.ok(
                bookService.getAllBooks()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBook(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                bookService.getBookById(id)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookResponse>> searchBooks(
            @RequestParam String query) {

        return ResponseEntity.ok(
                bookService.searchBooks(query)
        );
    }

    @GetMapping("/available")
    public ResponseEntity<List<BookResponse>> getAvailableBooks() {

        return ResponseEntity.ok(
                bookService.getAvailableBooks()
        );
    }

    @GetMapping("/author")
    public ResponseEntity<List<BookResponse>> getBooksByAuthor(
            @RequestParam String name) {

        return ResponseEntity.ok(
                bookService.getBooksByAuthor(name)
        );
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<BookResponse>> getBooksByCategory(
            @PathVariable Long categoryId) {

        return ResponseEntity.ok(
                bookService.getBooksByCategory(categoryId)
        );
    }

    @PostMapping
    public ResponseEntity<BookResponse> createBook(
            @Valid @RequestBody BookRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookService.createBook(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest request) {

        return ResponseEntity.ok(
                bookService.updateBook(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(
            @PathVariable Long id) {

        bookService.deleteBook(id);

        return ResponseEntity.noContent().build();
    }
}