package com.nexamarket.nexamarket.cart.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckoutCartRequest(@NotNull UUID customerId) {
}
