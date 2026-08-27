package com.nexamarket.stock.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateStockReservationRequest(
        @NotNull(message = "Varyant kimliği zorunludur") Long variantId,
        @NotNull(message = "Miktar zorunludur") @Positive(message = "Miktar sıfırdan büyük olmalıdır") Integer quantity) {
}
