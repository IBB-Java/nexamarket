package com.nexamarket.nexamarket.payment.application;

public interface StockReservationCommitGateway {

    void confirm(String reservationCode);
}
