package com.nexamarket.catalog.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateProductVariantRequest(
        @NotNull @Positive Long id,
        @DecimalMin(value = "0.01") BigDecimal price,
        @Min(0) Integer stockQuantity
) {
    @AssertTrue(message = "Varyant fiyatı veya stok miktarı güncellenmelidir")
    public boolean isChangeRequested() {
        return price != null || stockQuantity != null;
    }
}
