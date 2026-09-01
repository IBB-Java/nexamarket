package com.nexamarket.common.integration;

import java.math.BigDecimal;

public record CatalogVariantSnapshot(Long variantId, Long productId, Long sellerId,
                                     String productName, BigDecimal unitPrice) {
}
