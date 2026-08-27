package com.nexamarket.stock.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationExpirationScheduler {

    private final StockService stockService;

    @Scheduled(fixedDelayString = "${stock.reservation.expiration-check-interval:PT1M}")
    public void releaseExpiredReservations() {
        stockService.expireReservations();
    }
}
