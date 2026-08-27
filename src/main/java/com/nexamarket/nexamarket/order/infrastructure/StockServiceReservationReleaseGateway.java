package com.nexamarket.nexamarket.order.infrastructure;

import com.nexamarket.nexamarket.order.application.StockReservationReleaseGateway;
import com.nexamarket.stock.application.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockServiceReservationReleaseGateway implements StockReservationReleaseGateway {

    private final StockService stockService;

    @Override
    public void releaseReservation(String reservationCode) {
        stockService.releaseReservationInternally(reservationCode);
    }
}
