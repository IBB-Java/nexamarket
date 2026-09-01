package com.nexamarket.nexamarket.order.infrastructure;

import com.nexamarket.nexamarket.order.application.StockReservationReleaseGateway;
import com.nexamarket.common.integration.InternalRestClientFactory;
import org.springframework.stereotype.Component;

@Component
public class StockServiceReservationReleaseGateway implements StockReservationReleaseGateway {

    private final InternalRestClientFactory clients;

    public StockServiceReservationReleaseGateway(InternalRestClientFactory clients) {
        this.clients = clients;
    }

    @Override
    public void releaseReservation(String reservationCode) {
        clients.create("stock-service.base-url")
                .delete()
                .uri("/internal/stocks/reservations/{reservationCode}", reservationCode)
                .retrieve()
                .toBodilessEntity();
    }
}
