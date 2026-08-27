package com.nexamarket.catalog.application;

import java.time.Instant;

public record ProductCatalogChangedEvent(Long productId, Instant occurredAt) {

    public static ProductCatalogChangedEvent now(Long productId) {
        return new ProductCatalogChangedEvent(productId, Instant.now());
    }
}
