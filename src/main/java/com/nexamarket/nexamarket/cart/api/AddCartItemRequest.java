package com.nexamarket.nexamarket.cart.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull Long productVariantId,
        @Positive int quantity) {
}
