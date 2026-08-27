package com.nexamarket.stock.application;

public class StockVariantNotFoundException extends RuntimeException {
    public StockVariantNotFoundException(Long variantId) {
        super("Stok varyantı bulunamadı: " + variantId);
    }
}
