package com.smartlib.service;

import com.smartlib.dto.review.ReviewRequest;
import com.smartlib.dto.review.ReviewResponse;
import com.smartlib.entity.Book;
import com.smartlib.entity.Borrowing;
import com.smartlib.entity.Review;
import com.smartlib.entity.User;
import com.smartlib.enums.BorrowStatus;
import com.smartlib.exception.BadRequestException;
import com.smartlib.exception.ResourceNotFoundException;
import com.smartlib.repository.BookRepository;
import com.smartlib.repository.BorrowingRepository;
import com.smartlib.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final BorrowingRepository borrowingRepository;

    // ============================================================
    // CREATE REVIEW
    // ============================================================

    @Transactional
    public ReviewResponse createReview(
            ReviewRequest request,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        Book book = bookRepository
                .findById(request.getBookId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Book not found"
                        ));

        // --------------------------------------------------------
        // User must have borrowed the book
        // --------------------------------------------------------

        boolean hasBorrowed =
                borrowingRepository
                        .existsByUserIdAndBookIdAndStatus(
                                user.getId(),
                                book.getId(),
                                BorrowStatus.RETURNED
                        )
                ||
                borrowingRepository
                        .existsByUserIdAndBookIdAndStatus(
                                user.getId(),
                                book.getId(),
                                BorrowStatus.BORROWED
                        )
                ||
                borrowingRepository
                        .existsByUserIdAndBookIdAndStatus(
                                user.getId(),
                                book.getId(),
                                BorrowStatus.OVERDUE
                        );

        if (!hasBorrowed) {

            throw new BadRequestException(
                    "You can review a book only after borrowing it."
            );
        }

        // --------------------------------------------------------
        // One review per user per book
        // --------------------------------------------------------

        if (reviewRepository.existsByUserIdAndBookId(
                user.getId(),
                book.getId())) {

            throw new BadRequestException(
                    "You have already reviewed this book."
            );
        }

        Review review = Review.builder()
                .user(user)
                .book(book)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved =
                reviewRepository.save(review);

        updateBookRating(book);

        return toResponse(saved);
    }

    // ============================================================
    // GET REVIEWS FOR BOOK
    // ============================================================

    @Transactional(readOnly = true)
    public List<ReviewResponse> getBookReviews(
            Long bookId) {

        if (!bookRepository.existsById(bookId)) {

            throw new ResourceNotFoundException(
                    "Book not found"
            );
        }

        return reviewRepository
                .findByBookId(bookId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ============================================================
    // GET MY REVIEWS
    // ============================================================

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return reviewRepository
                .findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ============================================================
    // UPDATE REVIEW
    // ============================================================

    @Transactional
    public ReviewResponse updateReview(
            Long reviewId,
            ReviewRequest request,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        Review review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Review not found"
                                ));

        // --------------------------------------------------------
        // Ownership check
        // --------------------------------------------------------

        if (!review.getUser()
                .getId()
                .equals(user.getId())) {

            throw new BadRequestException(
                    "You cannot update another user's review."
            );
        }

        // --------------------------------------------------------
        // Book cannot be changed
        // --------------------------------------------------------

        if (!review.getBook()
                .getId()
                .equals(request.getBookId())) {

            throw new BadRequestException(
                    "The book of an existing review cannot be changed."
            );
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());

        Review saved =
                reviewRepository.save(review);

        updateBookRating(review.getBook());

        return toResponse(saved);
    }

    // ============================================================
    // DELETE REVIEW
    // ============================================================

    @Transactional
    public void deleteReview(
            Long reviewId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        Review review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Review not found"
                                ));

        if (!review.getUser()
                .getId()
                .equals(user.getId())) {

            throw new BadRequestException(
                    "You cannot delete another user's review."
            );
        }

        Book book = review.getBook();

        reviewRepository.delete(review);

        updateBookRating(book);
    }

    // ============================================================
    // UPDATE BOOK RATING
    // ============================================================

    private void updateBookRating(Book book) {

        List<Review> reviews =
                reviewRepository.findByBookId(
                        book.getId()
                );

        if (reviews.isEmpty()) {

            book.setAverageRating(0.0);
            book.setTotalRatings(0);

        } else {

            double average =
                    reviews.stream()
                            .mapToInt(Review::getRating)
                            .average()
                            .orElse(0.0);

            book.setAverageRating(
                    Math.round(average * 10.0) / 10.0
            );

            book.setTotalRatings(
                    reviews.size()
            );
        }

        bookRepository.save(book);
    }

    // ============================================================
    // CURRENT USER
    // ============================================================

    private User getCurrentUser(
            Authentication authentication) {

        return (User) authentication.getPrincipal();
    }

    // ============================================================
    // RESPONSE MAPPER
    // ============================================================

    private ReviewResponse toResponse(
            Review review) {

        return ReviewResponse.builder()
                .id(review.getId())
                .bookId(review.getBook().getId())
                .bookTitle(review.getBook().getTitle())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}