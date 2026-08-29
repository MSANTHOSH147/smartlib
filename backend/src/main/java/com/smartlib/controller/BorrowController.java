package com.smartlib.controller;

import com.smartlib.dto.borrowing.BorrowRequest;
import com.smartlib.dto.borrowing.BorrowResponse;
import com.smartlib.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrowings")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;


    // ============================================================
    // NORMAL BORROW
    // ============================================================

    @PostMapping
    public ResponseEntity<BorrowResponse> borrowBook(
            @Valid @RequestBody BorrowRequest request,
            Authentication authentication) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        borrowService.borrowBook(
                                request,
                                authentication
                        )
                );
    }


    // ============================================================
    // QR BORROW
    // ============================================================

    @PostMapping("/qr")
    public ResponseEntity<BorrowResponse> borrowBookByQr(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Request body is required."
            );
        }

        String qrToken =
                request.get("qrToken");

        if (qrToken == null
                || qrToken.isBlank()) {

            throw new IllegalArgumentException(
                    "QR token is required."
            );
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        borrowService.borrowBookByQr(
                                qrToken.trim(),
                                authentication
                        )
                );
    }


    // ============================================================
    // MY BORROWINGS
    // ============================================================

    @GetMapping("/my")
    public ResponseEntity<List<BorrowResponse>>
    getMyBorrowings(
            Authentication authentication) {

        return ResponseEntity.ok(
                borrowService.getMyBorrowings(
                        authentication
                )
        );
    }


    // ============================================================
    // MY ACTIVE BORROWINGS
    // ============================================================

    @GetMapping("/my/active")
    public ResponseEntity<List<BorrowResponse>>
    getMyActiveBorrowings(
            Authentication authentication) {

        return ResponseEntity.ok(
                borrowService.getMyActiveBorrowings(
                        authentication
                )
        );
    }


    // ============================================================
    // NORMAL RETURN
    // ============================================================

    @PostMapping("/{id}/return")
    public ResponseEntity<BorrowResponse> returnBook(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                borrowService.returnBook(
                        id,
                        authentication
                )
        );
    }


    // ============================================================
    // QR RETURN
    // ============================================================

    @PostMapping("/qr/return")
    public ResponseEntity<BorrowResponse>
    returnBookByQr(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Request body is required."
            );
        }

        String qrToken =
                request.get("qrToken");

        if (qrToken == null
                || qrToken.isBlank()) {

            throw new IllegalArgumentException(
                    "QR token is required."
            );
        }

        return ResponseEntity.ok(
                borrowService.returnBookByQr(
                        qrToken.trim(),
                        authentication
                )
        );
    }
}