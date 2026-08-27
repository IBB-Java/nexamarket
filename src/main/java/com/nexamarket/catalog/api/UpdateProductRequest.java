package com.nexamarket.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductRequest(
        @Size(max = 1000) String description,
        @DecimalMin(value = "0.01") BigDecimal basePrice,
        @Size(min = 1) List<@Valid UpdateProductVariantRequest> variants
) {
    @AssertTrue(message = "En az bir ürün alanı güncellenmelidir")
    public boolean isChangeRequested() {
        return description != null || basePrice != null || variants != null;
    }
}
