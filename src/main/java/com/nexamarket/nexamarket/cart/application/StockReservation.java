package com.nexamarket.nexamarket.cart.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockReservation(
        UUID reservationId,
        int reservedQuantity,
        BigDecimal unitPrice,
        Instant reservedUntil) {
}
