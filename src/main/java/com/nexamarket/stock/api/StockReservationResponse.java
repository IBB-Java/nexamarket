package com.nexamarket.stock.api;

import com.nexamarket.stock.entity.StockReservationStatus;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record StockReservationResponse(
        String reservationCode,
        Long variantId,
        Integer quantity,
        BigDecimal unitPrice,
        StockReservationStatus status,
        LocalDateTime expiresAt,
        Integer availableStock) {
}
