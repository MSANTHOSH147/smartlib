package com.smartlib.ai.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizedRecommendation {

    private Long bookId;
    private String title;
    private String author;
    private String categoryName;
    private String coverImageUrl;
    private String description;
    private Double averageRating;
    private Integer availableCopies;
    private Double relevanceScore;
    private boolean available;

    @Builder.Default
    private List<String> reasonSignals = new ArrayList<>();
}
