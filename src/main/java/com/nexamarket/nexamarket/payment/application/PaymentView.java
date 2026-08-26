package com.nexamarket.nexamarket.payment.application;

import com.nexamarket.nexamarket.payment.domain.PaymentTransaction;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentView(UUID paymentId, UUID orderId, String status,
                          BigDecimal walletAmount, BigDecimal cardAmount,
                          UUID providerPaymentId, int pollingAttempts) {

    public static PaymentView from(PaymentTransaction payment) {
        return new PaymentView(payment.getId(), payment.getOrderId(), payment.getStatus().name(),
                payment.getWalletAmount(), payment.getCardAmount(), payment.getProviderPaymentId(),
                payment.getPollingAttempts());
    }
}
