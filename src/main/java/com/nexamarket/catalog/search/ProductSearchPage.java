package com.nexamarket.catalog.search;

import java.util.List;

public record ProductSearchPage(
        List<ProductSearchDocument> items,
        long totalElements,
        int page,
        int size
) {
}
