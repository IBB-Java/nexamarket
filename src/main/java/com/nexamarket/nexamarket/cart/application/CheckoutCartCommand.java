package com.nexamarket.nexamarket.cart.application;

import java.util.List;

public record CheckoutCartCommand(Long customerId, String customerEmail, List<String> promotionCodes) {
    public CheckoutCartCommand(Long customerId) {
        this(customerId, null, List.of());
    }

    public CheckoutCartCommand(Long customerId, List<String> promotionCodes) {
        this(customerId, null, promotionCodes);
    }

    public CheckoutCartCommand {
        promotionCodes = promotionCodes == null ? List.of() : List.copyOf(promotionCodes);
    }
}
