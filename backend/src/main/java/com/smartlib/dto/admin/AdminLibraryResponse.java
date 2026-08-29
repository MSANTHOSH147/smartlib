package com.smartlib.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminLibraryResponse {

    private long totalBorrowings;

    private long activeBorrowings;

    private long overdueBorrowings;

    private long totalReservations;

    private long waitingReservations;

    private long readyReservations;

    private long totalFines;

    private long unpaidFines;
}