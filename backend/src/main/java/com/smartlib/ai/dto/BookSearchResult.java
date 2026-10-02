package com.smartlib.ai.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookSearchResult {

    private Long bookId;
    private String title;
    private String author;
    private String categoryName;
    private String isbn;
    private String publisher;
    private Integer publicationYear;
    private String description;
    private String coverImageUrl;
    private Integer totalCopies;
    private Integer availableCopies;
    private Double averageRating;
    private Double relevanceScore;
    private Double semanticScore;
    private Integer lexicalRank;
    private Integer semanticRank;
}
