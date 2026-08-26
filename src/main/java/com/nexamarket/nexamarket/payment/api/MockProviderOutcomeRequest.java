package com.nexamarket.nexamarket.payment.api;

import com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MockProviderOutcomeRequest(
        @NotNull ProviderPaymentStatus status,
        String failureReason,
        @NotNull @Min(0) @Max(3600) Long callbackDelaySeconds,
        @NotNull @Min(1) @Max(10) Integer duplicateDeliveries) {
}
