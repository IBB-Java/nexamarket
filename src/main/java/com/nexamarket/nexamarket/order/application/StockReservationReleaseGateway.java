package com.nexamarket.nexamarket.order.application;

/** Order timeout releases stock through the local stock module. */
public interface StockReservationReleaseGateway {

    void releaseReservation(String reservationCode);
}
