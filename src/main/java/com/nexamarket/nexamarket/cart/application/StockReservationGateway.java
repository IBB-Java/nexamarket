package com.nexamarket.nexamarket.cart.application;

import java.util.UUID;

/**
 * Cart communicates with the catalog service through this boundary rather than
 * reading or updating catalog stock tables directly.
 */
public interface StockReservationGateway {

    StockReservation createReservation(UUID customerId, UUID productVariantId, UUID sellerId, int quantity);

    StockReservation increaseReservation(UUID reservationId, int additionalQuantity);

    void releaseReservation(UUID reservationId);
}
