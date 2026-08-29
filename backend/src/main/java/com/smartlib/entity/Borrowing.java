package com.smartlib.entity;

import com.smartlib.enums.BorrowStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "borrowings",
    indexes = {
        @Index(name = "idx_borrowing_user", columnList = "user_id"),
        @Index(name = "idx_borrowing_book", columnList = "book_id"),
        @Index(name = "idx_borrowing_copy", columnList = "book_copy_id"),
        @Index(name = "idx_borrowing_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Borrowing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "book_id",
        nullable = false
    )
    private Book book;

    /*
     * Physical copy issued to this member.
     *
     * IMPORTANT:
     * nullable=false will be added only AFTER
     * existing borrowing records are migrated.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "book_copy_id"
    )
    private BookCopy bookCopy;

    @Column(nullable = false)
    private LocalDate borrowDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 20
    )
    @Builder.Default
    private BorrowStatus status =
            BorrowStatus.BORROWED;

    @Column(
        nullable = false,
        updatable = false
    )
    @Builder.Default
    private LocalDateTime createdAt =
            LocalDateTime.now();
}