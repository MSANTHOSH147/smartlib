package com.smartlib.dto.review;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewResponse {

    private Long id;

    private Long bookId;

    private String bookTitle;

    private Long userId;

    private String userName;

    private Integer rating;

    private String comment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}