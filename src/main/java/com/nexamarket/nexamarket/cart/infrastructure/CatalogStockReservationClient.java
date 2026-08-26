package com.nexamarket.nexamarket.cart.infrastructure;

import com.nexamarket.nexamarket.cart.application.StockReservation;
import com.nexamarket.nexamarket.cart.application.StockReservationGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** REST client for the catalog service's internal stock-reservation contract. */
@Component
public class CatalogStockReservationClient implements StockReservationGateway {

    private final RestClient restClient;

    public CatalogStockReservationClient(RestClient.Builder restClientBuilder,
                                         @Value("${catalog-service.base-url}") String catalogServiceBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(catalogServiceBaseUrl).build();
    }

    @Override
    public StockReservation createReservation(UUID customerId, UUID productVariantId, UUID sellerId, int quantity) {
        ReservationResponse response = restClient.post()
                .uri("/internal/stock-reservations")
                .body(new CreateReservationRequest(customerId, productVariantId, sellerId, quantity))
                .retrieve()
                .body(ReservationResponse.class);
        return toReservation(response);
    }

    @Override
    public StockReservation increaseReservation(UUID reservationId, int additionalQuantity) {
        ReservationResponse response = restClient.patch()
                .uri("/internal/stock-reservations/{reservationId}", reservationId)
                .body(new IncreaseReservationRequest(additionalQuantity))
                .retrieve()
                .body(ReservationResponse.class);
        return toReservation(response);
    }

    private StockReservation toReservation(ReservationResponse response) {
        if (response == null) {
            throw new IllegalStateException("Catalog service returned an empty stock-reservation response.");
        }
        return new StockReservation(
                response.reservationId(),
                response.reservedQuantity(),
                response.unitPrice(),
                response.reservedUntil());
    }

    private record CreateReservationRequest(UUID customerId, UUID productVariantId, UUID sellerId, int quantity) {
    }

    private record IncreaseReservationRequest(int additionalQuantity) {
    }

    private record ReservationResponse(UUID reservationId, int reservedQuantity, BigDecimal unitPrice,
                                       Instant reservedUntil) {
    }
}
