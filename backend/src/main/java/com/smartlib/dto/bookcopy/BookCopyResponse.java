package com.smartlib.dto.bookcopy;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BookCopyResponse {

    private Long id;

    private Long bookId;

    private String bookTitle;

    private String author;

    private String isbn;

    private String copyNumber;

    private String status;

    private String condition;

    private String location;

    private String qrToken;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}