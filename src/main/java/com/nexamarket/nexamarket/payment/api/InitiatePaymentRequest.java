package com.nexamarket.nexamarket.payment.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiatePaymentRequest(
        @NotNull UUID orderId,
        @NotBlank String idempotencyKey,
        @NotNull @DecimalMin(value = "0.00") BigDecimal walletAmount,
        @NotNull @DecimalMin(value = "0.00") BigDecimal cardAmount) {
}
