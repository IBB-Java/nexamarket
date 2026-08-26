package com.nexamarket.catalog.api;

import com.nexamarket.catalog.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.Map;

public record ProductVariantResponse(
        Long id,
        String sku,
        Map<String, String> attributes,
        BigDecimal price,
        Integer stockQuantity
) {
    public static ProductVariantResponse from(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getSku(),
                Map.copyOf(variant.getAttributes()),
                variant.getPrice(),
                variant.getStockQuantity()
        );
    }
}
