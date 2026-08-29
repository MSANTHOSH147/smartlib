package com.smartlib.entity;

import com.smartlib.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservations",
        indexes = {
                @Index(
                        name = "idx_reservation_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_reservation_book",
                        columnList = "book_id"
                ),
                @Index(
                        name = "idx_reservation_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_reservation_book_copy",
                        columnList = "book_copy_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // MEMBER
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    // ============================================================
    // BOOK
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "book_id",
            nullable = false
    )
    private Book book;

    // ============================================================
    // PHYSICAL COPY
    //
    // NULL while WAITING.
    // Set when reservation becomes READY.
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "book_copy_id"
    )
    private BookCopy bookCopy;

    // ============================================================
    // STATUS
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private ReservationStatus status =
            ReservationStatus.WAITING;

    // ============================================================
    // TIMESTAMPS
    // ============================================================

    @Column(
            nullable = false,
            updatable = false
    )
    @Builder.Default
    private LocalDateTime reservedAt =
            LocalDateTime.now();

    private LocalDateTime readyAt;
}