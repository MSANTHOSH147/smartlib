package com.smartlib.dto.reservation;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationResponse {

    private Long id;

    private Long bookId;

    private String bookTitle;

    private Long userId;

    private String userName;

    private String status;

    private LocalDateTime reservedAt;

    private LocalDateTime readyAt;
}