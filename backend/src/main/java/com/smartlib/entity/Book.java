package com.smartlib.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "books",
    indexes = {
        @Index(name = "idx_book_title", columnList = "title"),
        @Index(name = "idx_book_author", columnList = "author"),
        @Index(name = "idx_book_isbn", columnList = "isbn")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 150)
    private String author;

    @Column(unique = true, length = 20)
    private String isbn;

    @Column(length = 100)
    private String publisher;

    private Integer publicationYear;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String coverImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalCopies = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer availableCopies = 1;

    @Column(nullable = false)
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalRatings = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "book")
    @Builder.Default
    private List<Borrowing> borrowings = new ArrayList<>();

    @OneToMany(mappedBy = "book")
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "book")
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(
    mappedBy = "book",
    cascade = CascadeType.ALL,
    orphanRemoval = true
)
@Builder.Default
private List<BookCopy> copies = new ArrayList<>();
}
