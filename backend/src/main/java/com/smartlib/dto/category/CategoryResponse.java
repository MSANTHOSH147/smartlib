package com.smartlib.dto.category;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {

    private Long id;

    private String name;

    private String description;
}