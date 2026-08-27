package com.nexamarket.stock.api;

public record StockLevelResponse(Long variantId, String sku, Integer availableStock) {
}
