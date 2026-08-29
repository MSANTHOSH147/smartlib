package com.smartlib.controller;

import com.smartlib.dto.bookcopy.BookCopyResponse;
import com.smartlib.dto.borrowing.BorrowResponse;
import com.smartlib.service.BookCopyService;
import com.smartlib.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import com.smartlib.dto.bookcopy.UpdateBookCopyRequest;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/admin/book-copies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookCopyController {

    private final BookCopyService bookCopyService;

    private final BorrowService borrowService;


    // ============================================================
    // ADMIN QR LOOKUP
    // ============================================================

    @GetMapping("/qr/{qrToken}")
    public ResponseEntity<BookCopyResponse> getByQrToken(
            @PathVariable String qrToken) {

        return ResponseEntity.ok(
                bookCopyService.getByQrToken(qrToken)
        );
    }


    // ============================================================
    // ADMIN QR RETURN
    // ============================================================

    @PostMapping("/qr/{qrToken}/return")
    public ResponseEntity<BorrowResponse> returnByQr(
            @PathVariable String qrToken) {

        return ResponseEntity.ok(
                borrowService.returnBookByAdminQr(
                        qrToken
                )
        );
    }

    // ============================================================
// ADMIN DEACTIVATE PHYSICAL COPY
// ============================================================

@DeleteMapping("/{id}")
public ResponseEntity<Void> deactivateCopy(
        @PathVariable Long id) {

    borrowService.deactivateBookCopy(id);

    return ResponseEntity.noContent().build();
}

// ============================================================
// ADMIN UPDATE PHYSICAL COPY
// ============================================================

@PutMapping("/{id}")
public ResponseEntity<BookCopyResponse> updateCopy(
        @PathVariable Long id,
        @Valid @RequestBody UpdateBookCopyRequest request
) {

    return ResponseEntity.ok(
            bookCopyService.updateCopy(
                    id,
                    request
            )
    );
}
}