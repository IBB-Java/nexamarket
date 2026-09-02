package com.nexamarket.catalog.api;

import com.nexamarket.catalog.search.ProductSearchDocument;

import java.math.BigDecimal;
import java.util.List;
import java.io.Serializable;

public record ProductSearchItemResponse(
        Long id,
        Long sellerId,
        String sellerName,
        String name,
        String description,
        List<Long> categoryIds,
        List<String> categoryNames,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal sellerRating,
        long totalStock,
        boolean inStock,
        String imageUrl
) implements Serializable {
    public static ProductSearchItemResponse from(ProductSearchDocument document, String sellerName, String imageUrl) {
        return new ProductSearchItemResponse(
                Long.valueOf(document.getId()),
                document.getSellerId(),
                sellerName,
                document.getName(),
                document.getDescription(),
                List.copyOf(document.getCategoryIds()),
                List.copyOf(document.getCategoryNames()),
                BigDecimal.valueOf(document.getMinPrice()),
                BigDecimal.valueOf(document.getMaxPrice()),
                document.getSellerRating() == null ? null : BigDecimal.valueOf(document.getSellerRating()),
                document.getTotalStock() == null ? 0 : document.getTotalStock(),
                Boolean.TRUE.equals(document.getInStock()),
                imageUrl
        );
    }
}
