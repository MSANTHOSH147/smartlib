package com.smartlib.dto.borrowing;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class BorrowResponse {

    private Long id;

    private Long bookId;

    private String bookTitle;

    private Long userId;

    private String userName;

    private LocalDate borrowDate;

    private LocalDate dueDate;

    private LocalDate returnDate;

    private String status;
}