package com.nexamarket.nexamarket.payment.application;

import com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus;

import java.util.UUID;

public record PaymentWebhookCommand(String providerEventId, UUID providerPaymentId,
                                    ProviderPaymentStatus status, String failureReason) {
}
