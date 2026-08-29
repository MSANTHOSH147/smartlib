package com.smartlib.service;

import com.smartlib.dto.category.CategoryRequest;
import com.smartlib.dto.category.CategoryResponse;
import com.smartlib.entity.Category;
import com.smartlib.exception.BadRequestException;
import com.smartlib.exception.ResourceNotFoundException;
import com.smartlib.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getAllCategories() {

        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {

        return toResponse(
                categoryRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found with id: " + id
                                ))
        );
    }

    public CategoryResponse createCategory(
            CategoryRequest request) {

        if (categoryRepository.existsByNameIgnoreCase(
                request.getName())) {

            throw new BadRequestException(
                    "Category already exists"
            );
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return toResponse(
                categoryRepository.save(category)
        );
    }

    public CategoryResponse updateCategory(
            Long id,
            CategoryRequest request) {

        Category category =
                categoryRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found"
                                ));

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return toResponse(
                categoryRepository.save(category)
        );
    }

    public void deleteCategory(Long id) {

        Category category =
                categoryRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found"
                                ));

        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(
            Category category) {

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}