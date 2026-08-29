package com.smartlib.dto.book;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookResponse {

    private Long id;

    private String title;

    private String author;

    private String isbn;

    private String publisher;

    private Integer publicationYear;

    private String description;

    private String coverImageUrl;

    private Integer totalCopies;

    private Integer availableCopies;

    private Double averageRating;

    private Integer totalRatings;

    private Long categoryId;

    private String categoryName;
}