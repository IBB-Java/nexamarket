package com.nexamarket.nexamarket.cart.application;

import java.util.UUID;

public record AddCartItemCommand(
        UUID customerId,
        UUID productVariantId,
        UUID sellerId,
        int quantity) {
}
