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
@RequestMapping("/api/admin/books")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookService bookService;

    // ============================================================
    // GET ALL BOOKS
    // ============================================================

    @GetMapping
    public ResponseEntity<List<BookResponse>> getAllBooks() {

        return ResponseEntity.ok(
                bookService.getAllBooks()
        );
    }

    // ============================================================
    // GET BOOK BY ID
    // ============================================================

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBook(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                bookService.getBookById(id)
        );
    }

    // ============================================================
    // CREATE BOOK
    // ============================================================

    @PostMapping
    public ResponseEntity<BookResponse> createBook(
            @Valid @RequestBody BookRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        bookService.createBook(request)
                );
    }

    // ============================================================
    // UPDATE BOOK
    // ============================================================

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest request) {

        return ResponseEntity.ok(
                bookService.updateBook(
                        id,
                        request
                )
        );
    }

    // ============================================================
    // DELETE BOOK
    // ============================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(
            @PathVariable Long id) {

        bookService.deleteBook(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}