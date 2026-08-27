package com.nexamarket.promotion.api;

import com.nexamarket.promotion.entity.PromotionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreatePromotionRequest(
        @NotBlank String code,
        @NotNull PromotionType type,
        @NotNull @DecimalMin(value = "0.01") BigDecimal value,
        @DecimalMin(value = "0.00") BigDecimal minimumOrderAmount,
        boolean stackable,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) {
}
