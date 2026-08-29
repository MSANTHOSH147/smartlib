package com.smartlib.entity;

import com.smartlib.enums.FineStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowing_id", nullable = false, unique = true)
    private Borrowing borrowing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FineStatus status = FineStatus.UNPAID;

    private LocalDateTime paidAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}