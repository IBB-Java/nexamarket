package com.nexamarket.nexamarket.cart.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID customerId,
        @NotNull UUID productVariantId,
        @NotNull UUID sellerId,
        @Positive int quantity) {
}
