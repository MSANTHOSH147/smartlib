package com.smartlib.controller;

import com.smartlib.dto.review.ReviewRequest;
import com.smartlib.dto.review.ReviewResponse;
import com.smartlib.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ============================================================
    // GET BOOK REVIEWS
    // PUBLIC - anyone can read reviews
    // ============================================================

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<ReviewResponse>> getBookReviews(
            @PathVariable Long bookId) {

        return ResponseEntity.ok(
                reviewService.getBookReviews(bookId)
        );
    }

    // ============================================================
    // CREATE REVIEW
    // MEMBER / ADMIN
    // ============================================================

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        reviewService.createReview(
                                request,
                                authentication
                        )
                );
    }

    // ============================================================
    // GET MY REVIEWS
    // MEMBER / ADMIN
    // ============================================================

    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(
            Authentication authentication) {

        return ResponseEntity.ok(
                reviewService.getMyReviews(
                        authentication
                )
        );
    }

    // ============================================================
    // UPDATE REVIEW
    // MEMBER / ADMIN
    // ============================================================

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                reviewService.updateReview(
                        id,
                        request,
                        authentication
                )
        );
    }

    // ============================================================
    // DELETE REVIEW
    // MEMBER / ADMIN
    // ============================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            Authentication authentication) {

        reviewService.deleteReview(
                id,
                authentication
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}