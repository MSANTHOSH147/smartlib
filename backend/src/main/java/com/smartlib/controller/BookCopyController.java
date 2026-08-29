package com.smartlib.controller;

import com.smartlib.dto.bookcopy.AdminBookCopyScanResponse;
import com.smartlib.dto.bookcopy.BookCopyResponse;
import com.smartlib.service.BookCopyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/book-copies")
@RequiredArgsConstructor
public class BookCopyController {

    private final BookCopyService bookCopyService;


    // ============================================================
    // MEMBER QR LOOKUP
    // ============================================================

    @GetMapping("/qr/{qrToken}")
    public ResponseEntity<BookCopyResponse> getByQrToken(
            @PathVariable String qrToken) {

        return ResponseEntity.ok(
                bookCopyService.getByQrToken(qrToken)
        );
    }


    // ============================================================
    // ADMIN QR SCAN
    // ============================================================

    @GetMapping("/admin/qr/{qrToken}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminBookCopyScanResponse> scanForAdmin(
            @PathVariable String qrToken) {

        return ResponseEntity.ok(
                bookCopyService.scanForAdmin(qrToken)
        );
    }


    // ============================================================
    // GET ALL COPIES FOR A BOOK
    // ============================================================

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<BookCopyResponse>> getByBookId(
            @PathVariable Long bookId) {

        return ResponseEntity.ok(
                bookCopyService.getByBookId(bookId)
        );
    }


    // ============================================================
    // GET COPY BY ID
    // ============================================================

    @GetMapping("/{id}")
    public ResponseEntity<BookCopyResponse> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                bookCopyService.getById(id)
        );
    }
}