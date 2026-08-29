package com.smartlib.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationRequest {

    @NotNull(message = "Book ID is required")
    private Long bookId;
}