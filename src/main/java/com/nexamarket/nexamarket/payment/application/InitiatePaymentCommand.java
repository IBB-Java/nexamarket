package com.nexamarket.nexamarket.payment.application;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiatePaymentCommand(UUID orderId, String idempotencyKey,
                                     BigDecimal walletAmount, BigDecimal cardAmount) {
}
