package com.smartlib.dto.bookcopy;

import com.smartlib.enums.BookCopyCondition;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBookCopyRequest {

    private BookCopyCondition condition;

    @Size(
            max = 100,
            message = "Location cannot exceed 100 characters."
    )
    private String location;
}