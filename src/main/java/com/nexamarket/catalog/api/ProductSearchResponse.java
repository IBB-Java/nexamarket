package com.nexamarket.catalog.api;

import com.nexamarket.catalog.search.ProductSearchPage;

import java.util.List;
import java.io.Serializable;

public record ProductSearchResponse(
        List<ProductSearchItemResponse> items,
        long totalElements,
        int page,
        int size,
        int totalPages
) implements Serializable {
    public static ProductSearchResponse from(ProductSearchPage result) {
        int totalPages = result.size() == 0 ? 0 : (int) Math.ceil(result.totalElements() / (double) result.size());
        return new ProductSearchResponse(
                result.items().stream().map(ProductSearchItemResponse::from).toList(),
                result.totalElements(),
                result.page(),
                result.size(),
                totalPages
        );
    }
}
