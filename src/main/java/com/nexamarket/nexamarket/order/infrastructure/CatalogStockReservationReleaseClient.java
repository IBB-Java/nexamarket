package com.nexamarket.nexamarket.order.infrastructure;

import com.nexamarket.nexamarket.order.application.StockReservationReleaseGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class CatalogStockReservationReleaseClient implements StockReservationReleaseGateway {

    private final RestClient restClient;

    public CatalogStockReservationReleaseClient(RestClient.Builder restClientBuilder,
                                                @Value("${catalog-service.base-url}") String catalogServiceBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(catalogServiceBaseUrl).build();
    }

    @Override
    public void releaseReservation(UUID reservationId) {
        restClient.delete()
                .uri("/internal/stock-reservations/{reservationId}", reservationId)
                .retrieve()
                .toBodilessEntity();
    }
}
