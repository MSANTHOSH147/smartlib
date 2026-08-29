package com.smartlib.repository;

import com.smartlib.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository
        extends JpaRepository<Review, Long> {

    List<Review> findByBookId(Long bookId);

    List<Review> findByUserId(Long userId);

    Optional<Review> findByUserIdAndBookId(
            Long userId,
            Long bookId
    );

    boolean existsByUserIdAndBookId(
            Long userId,
            Long bookId
    );
}