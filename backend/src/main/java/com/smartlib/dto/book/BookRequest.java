package com.smartlib.dto.book;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    private String isbn;

    private String publisher;

    private Integer publicationYear;

    private String description;

    private String coverImageUrl;

    @NotNull(message = "Total copies is required")
    @Min(value = 1, message = "Total copies must be at least 1")
    private Integer totalCopies;

    @NotNull(message = "Category is required")
    private Long categoryId;
}