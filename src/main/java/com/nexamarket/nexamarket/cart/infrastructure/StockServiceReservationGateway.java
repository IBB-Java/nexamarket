package com.nexamarket.nexamarket.cart.infrastructure;

import com.nexamarket.common.integration.InternalRestClientFactory;
import com.nexamarket.nexamarket.cart.application.StockReservation;
import com.nexamarket.nexamarket.cart.application.StockReservationGateway;
import com.nexamarket.stock.api.StockInternalController;
import com.nexamarket.stock.api.StockReservationResponse;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class StockServiceReservationGateway implements StockReservationGateway {

    private final InternalRestClientFactory clients;

    public StockServiceReservationGateway(InternalRestClientFactory clients) {
        this.clients = clients;
    }

    @Override
    public StockReservation createReservation(Long customerId, Long productVariantId, int quantity) {
        StockReservationResponse response = clients.create("stock-service.base-url")
                .post()
                .uri("/internal/stocks/reservations")
                .body(new StockInternalController.InternalReservationRequest(customerId, productVariantId, quantity))
                .retrieve()
                .body(StockReservationResponse.class);
        return toCartReservation(response);
    }

    @Override
    public StockReservation increaseReservation(String reservationCode, int additionalQuantity) {
        StockReservationResponse response = clients.create("stock-service.base-url")
                .patch()
                .uri("/internal/stocks/reservations/{reservationCode}", reservationCode)
                .body(new StockInternalController.IncreaseReservationRequest(additionalQuantity))
                .retrieve()
                .body(StockReservationResponse.class);
        return toCartReservation(response);
    }

    @Override
    public StockReservation decreaseReservation(String reservationCode, int quantity) {
        StockReservationResponse response = clients.create("stock-service.base-url")
                .patch()
                .uri("/internal/stocks/reservations/{reservationCode}/decrement", reservationCode)
                .body(new StockInternalController.DecreaseReservationRequest(quantity))
                .retrieve()
                .body(StockReservationResponse.class);
        return toCartReservation(response);
    }

    @Override
    public void releaseReservation(String reservationCode) {
        clients.create("stock-service.base-url")
                .delete()
                .uri("/internal/stocks/reservations/{reservationCode}", reservationCode)
                .retrieve()
                .toBodilessEntity();
    }

    private StockReservation toCartReservation(StockReservationResponse response) {
        if (response == null) {
            throw new IllegalStateException("Stock service returned an empty reservation response.");
        }
        return new StockReservation(response.reservationCode(), response.quantity(), response.unitPrice(),
                response.expiresAt().atZone(ZoneOffset.UTC).toInstant());
    }
}
