package com.smartlib.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_book_review",
            columnNames = {"user_id", "book_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}