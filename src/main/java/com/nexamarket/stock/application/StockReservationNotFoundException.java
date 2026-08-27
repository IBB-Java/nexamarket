package com.nexamarket.stock.application;

public class StockReservationNotFoundException extends RuntimeException {
    public StockReservationNotFoundException(String reservationCode) {
        super("Stok rezervasyonu bulunamadı: " + reservationCode);
    }
}
