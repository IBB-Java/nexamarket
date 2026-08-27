package com.nexamarket.stock.application;

public class StockAccessDeniedException extends RuntimeException {
    public StockAccessDeniedException() {
        super("Bu stok kaynağı üzerinde işlem yetkiniz yok");
    }
}
