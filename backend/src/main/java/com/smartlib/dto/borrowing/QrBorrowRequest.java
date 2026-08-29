package com.smartlib.dto.borrowing;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QrBorrowRequest {

    @NotBlank(message = "QR token is required")
    private String qrToken;
}