package com.nexamarket.nexamarket.cart.application;

/** Boundary between the cart aggregate and the atomic stock-reservation module. */
public interface StockReservationGateway {

    StockReservation createReservation(Long customerId, Long productVariantId, int quantity);

    StockReservation increaseReservation(String reservationCode, int additionalQuantity);

    StockReservation decreaseReservation(String reservationCode, int quantity);

    void releaseReservation(String reservationCode);
}
