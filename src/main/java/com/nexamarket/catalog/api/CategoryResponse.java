package com.nexamarket.catalog.api;

import com.nexamarket.catalog.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        Long parentCategoryId
) {
    public static CategoryResponse from(Category category) {
        Long parentId = category.getParent() == null ? null : category.getParent().getId();
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(), parentId);
    }
}
