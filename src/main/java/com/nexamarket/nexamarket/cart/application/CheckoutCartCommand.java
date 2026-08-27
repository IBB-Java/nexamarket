package com.nexamarket.nexamarket.cart.application;

import java.util.List;

public record CheckoutCartCommand(Long customerId, List<String> promotionCodes) {
    public CheckoutCartCommand(Long customerId) {
        this(customerId, List.of());
    }

    public CheckoutCartCommand {
        promotionCodes = promotionCodes == null ? List.of() : List.copyOf(promotionCodes);
    }
}
