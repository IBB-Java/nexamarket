package com.nexamarket.catalog.api;

import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.entity.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        Long sellerId,
        String name,
        String description,
        BigDecimal basePrice,
        ProductStatus status,
        List<CategoryResponse> categories,
        List<ProductVariantResponse> variants
) {
    public static ProductResponse from(Product product) {
        List<CategoryResponse> categories = product.getCategories().stream()
                .map(CategoryResponse::from)
                .sorted((left, right) -> left.id().compareTo(right.id()))
                .toList();
        List<ProductVariantResponse> variants = product.getVariants().stream()
                .map(ProductVariantResponse::from)
                .sorted((left, right) -> left.id().compareTo(right.id()))
                .toList();
        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getStatus(),
                categories,
                variants
        );
    }
}
