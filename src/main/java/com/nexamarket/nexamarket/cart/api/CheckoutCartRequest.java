package com.nexamarket.nexamarket.cart.api;

import java.util.List;

/** Checkout uses the authenticated customer; no client-controlled customer id is accepted. */
public record CheckoutCartRequest(List<String> promotionCodes) {

    public CheckoutCartRequest {
        promotionCodes = promotionCodes == null ? List.of() : List.copyOf(promotionCodes);
    }
}
