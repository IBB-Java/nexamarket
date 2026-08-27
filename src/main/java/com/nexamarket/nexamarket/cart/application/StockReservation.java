package com.nexamarket.nexamarket.cart.application;

import java.math.BigDecimal;
import java.time.Instant;

public record StockReservation(
        String reservationCode,
        int reservedQuantity,
        BigDecimal unitPrice,
        Instant reservedUntil) {
}
