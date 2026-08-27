package com.nexamarket.nexamarket.payment.application;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiatePaymentCommand(UUID orderId, Long customerId, String idempotencyKey,
                                     BigDecimal walletAmount, BigDecimal cardAmount) {

    /** Retained for application-level tests and trusted internal callers. */
    public InitiatePaymentCommand(UUID orderId, String idempotencyKey,
                                  BigDecimal walletAmount, BigDecimal cardAmount) {
        this(orderId, null, idempotencyKey, walletAmount, cardAmount);
    }
}
