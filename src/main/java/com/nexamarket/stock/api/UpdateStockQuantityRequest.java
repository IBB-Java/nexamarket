package com.nexamarket.stock.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStockQuantityRequest(
        @NotNull(message = "Stok miktarı zorunludur")
        @PositiveOrZero(message = "Stok miktarı negatif olamaz") Integer stockQuantity) {
}
