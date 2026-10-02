package com.smartlib.ai.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexingResult {

    private int indexed;
    private int skipped;
    private int failed;
    private int total;

    @Builder.Default
    private List<String> failureDetails = new ArrayList<>();

    public void incrementIndexed() {
        this.indexed++;
    }

    public void incrementSkipped() {
        this.skipped++;
    }

    public void incrementFailed(Long bookId, String reason) {
        this.failed++;
        if (failureDetails == null) {
            failureDetails = new ArrayList<>();
        }
        failureDetails.add("Book ID " + bookId + ": " + reason);
    }
}
