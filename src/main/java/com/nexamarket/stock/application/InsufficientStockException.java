package com.nexamarket.stock.application;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long variantId) {
        super("Varyant için yeterli kullanılabilir stok yok: " + variantId);
    }
}
