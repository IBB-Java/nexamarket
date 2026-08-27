package com.nexamarket.nexamarket.cart.infrastructure;

import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.catalog.repository.ProductVariantRepository;
import com.nexamarket.nexamarket.cart.application.StockReservation;
import com.nexamarket.nexamarket.cart.application.StockReservationGateway;
import com.nexamarket.stock.api.CreateStockReservationRequest;
import com.nexamarket.stock.api.StockReservationResponse;
import com.nexamarket.stock.application.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class StockServiceReservationGateway implements StockReservationGateway {

    private final StockService stockService;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public StockReservation createReservation(Long customerId, Long productVariantId, int quantity) {
        StockReservationResponse response = stockService.reserve(
                new CreateStockReservationRequest(productVariantId, quantity),
                new AuthPrincipal(customerId, "cart-customer-" + customerId, UserRole.CUSTOMER));
        return toCartReservation(response);
    }

    @Override
    public StockReservation increaseReservation(String reservationCode, int additionalQuantity) {
        return toCartReservation(stockService.increaseReservationInternally(reservationCode, additionalQuantity));
    }

    @Override
    public void releaseReservation(String reservationCode) {
        stockService.releaseReservationInternally(reservationCode);
    }

    private StockReservation toCartReservation(StockReservationResponse response) {
        var variant = productVariantRepository.findById(response.variantId())
                .orElseThrow(() -> new IllegalStateException("Reserved product variant was not found."));
        return new StockReservation(response.reservationCode(), response.quantity(), variant.getPrice(),
                response.expiresAt().atZone(ZoneOffset.UTC).toInstant());
    }
}
