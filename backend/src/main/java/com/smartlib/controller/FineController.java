package com.smartlib.controller;

import com.smartlib.dto.fine.FineResponse;
import com.smartlib.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;


    // ============================================================
    // GET ALL MY FINES
    // ============================================================

    @GetMapping("/my")
    public ResponseEntity<List<FineResponse>> getMyFines(
            Authentication authentication) {

        return ResponseEntity.ok(
                fineService.getMyFines(authentication)
        );
    }


    // ============================================================
    // GET MY UNPAID FINES
    // ============================================================

    @GetMapping("/my/unpaid")
    public ResponseEntity<List<FineResponse>> getMyUnpaidFines(
            Authentication authentication) {

        return ResponseEntity.ok(
                fineService.getMyUnpaidFines(authentication)
        );
    }


    // ============================================================
    // CALCULATE FINE
    // ============================================================

    @PostMapping("/borrowing/{borrowingId}/calculate")
    public ResponseEntity<FineResponse> calculateFine(
            @PathVariable Long borrowingId,
            Authentication authentication) {

        return ResponseEntity.ok(
                fineService.calculateFine(
                        borrowingId,
                        authentication
                )
        );
    }


    // ============================================================
    // PAY FINE
    // ============================================================

    @PostMapping("/{fineId}/pay")
    public ResponseEntity<FineResponse> payFine(
            @PathVariable Long fineId,
            Authentication authentication) {

        return ResponseEntity.ok(
                fineService.payFine(
                        fineId,
                        authentication
                )
        );
    }
}