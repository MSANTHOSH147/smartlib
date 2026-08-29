package com.smartlib.dto.bookcopy;

import com.smartlib.dto.borrowing.BorrowResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminBookCopyScanResponse {

    private BookCopyResponse copy;

    private BorrowResponse activeBorrowing;

    private boolean borrowed;

    private boolean available;
}