package com.nexamarket.catalog.api;

import com.nexamarket.catalog.search.ProductSearchPage;

import java.util.List;
import java.util.Map;
import java.io.Serializable;

public record ProductSearchResponse(
        List<ProductSearchItemResponse> items,
        long totalElements,
        int page,
        int size,
        int totalPages
) implements Serializable {
    public static ProductSearchResponse from(ProductSearchPage result, Map<Long, String> sellerNames) {
        int totalPages = result.size() == 0 ? 0 : (int) Math.ceil(result.totalElements() / (double) result.size());
        return new ProductSearchResponse(
                result.items().stream()
                        .map(document -> ProductSearchItemResponse.from(document,
                                sellerNames.getOrDefault(document.getSellerId(), "Satıcı #" + document.getSellerId())))
                        .toList(),
                result.totalElements(),
                result.page(),
                result.size(),
                totalPages
        );
    }
}
