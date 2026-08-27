package com.nexamarket.stock.api;

import com.nexamarket.stock.entity.StockReservationStatus;

import java.time.LocalDateTime;

public record StockReservationResponse(
        String reservationCode,
        Long variantId,
        Integer quantity,
        StockReservationStatus status,
        LocalDateTime expiresAt,
        Integer availableStock) {
}
