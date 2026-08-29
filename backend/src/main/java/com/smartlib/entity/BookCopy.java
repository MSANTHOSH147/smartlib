package com.smartlib.entity;

import com.smartlib.enums.BookCopyCondition;
import com.smartlib.enums.BookCopyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "book_copies",
        indexes = {
                @Index(
                        name = "idx_book_copy_book",
                        columnList = "book_id"
                ),
                @Index(

                        name = "idx_book_copy_status",
                        columnList = "status"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_book_copy_number",
                        columnNames = {"book_id", "copy_number"}
                ),
                @UniqueConstraint(
                        name = "uk_book_copy_qr_token",
                        columnNames = {"qr_token"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "book_id",
            nullable = false
    )
    private Book book;

    @Column(
            name = "copy_number",
            nullable = false,
            length = 50
    )
    private String copyNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private BookCopyStatus status = BookCopyStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
@Column(
        name = "book_condition",
        nullable = false,
        length = 20
)
@Builder.Default
private BookCopyCondition condition = BookCopyCondition.GOOD;

    @Column(length = 100)
    private String location;

    @Column(
            name = "qr_token",
            nullable = false,
            unique = true,
            length = 100
    )
    private String qrToken;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
