package com.nexamarket.catalog.search;

import java.math.BigDecimal;

public record ProductSearchCriteria(
        String query,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minSellerRating,
        int page,
        int size
) {
}
