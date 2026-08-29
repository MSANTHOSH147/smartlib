package com.smartlib.dto.fine;

import com.smartlib.enums.FineStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FineResponse {

    private Long id;

    private Long borrowingId;

    private Long userId;

    private String userName;

    private Long bookId;

    private String bookTitle;

    private Double amount;

    private FineStatus status;

    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
}