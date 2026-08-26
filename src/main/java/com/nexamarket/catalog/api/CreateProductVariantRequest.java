package com.nexamarket.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record CreateProductVariantRequest(
        @NotBlank @Size(max = 100) String sku,
        @NotNull Map<@NotBlank String, @NotBlank String> attributes,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @NotNull @PositiveOrZero Integer stockQuantity
) {
}
