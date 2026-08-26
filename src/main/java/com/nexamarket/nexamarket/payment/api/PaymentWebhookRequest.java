package com.nexamarket.nexamarket.payment.api;

import com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentWebhookRequest(
        @NotBlank String providerEventId,
        @NotNull UUID providerPaymentId,
        @NotNull ProviderPaymentStatus status,
        String failureReason) {
}
