package com.smartlib.dto.borrowing;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BorrowRequest {

    @NotNull(message = "Book ID is required")
    private Long bookId;
}