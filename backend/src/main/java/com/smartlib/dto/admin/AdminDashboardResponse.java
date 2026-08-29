package com.smartlib.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardResponse {

    private long totalBooks;

    private long totalMembers;

    private long activeBorrowings;

    private long overdueBorrowings;

    private long pendingReservations;

    private long unpaidFines;
}