package com.nexamarket.nexamarket.payment.infrastructure;

import com.nexamarket.common.integration.InternalRestClientFactory;
import com.nexamarket.nexamarket.payment.application.StockReservationCommitGateway;
import org.springframework.stereotype.Component;

@Component
public class RestStockReservationCommitGateway implements StockReservationCommitGateway {

    private final InternalRestClientFactory clients;

    public RestStockReservationCommitGateway(InternalRestClientFactory clients) {
        this.clients = clients;
    }

    @Override
    public void confirm(String reservationCode) {
        clients.create("stock-service.base-url")
                .post()
                .uri("/internal/stocks/reservations/{reservationCode}/confirm", reservationCode)
                .retrieve()
                .toBodilessEntity();
    }
}
