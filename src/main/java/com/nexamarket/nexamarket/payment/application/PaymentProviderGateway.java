package com.nexamarket.nexamarket.payment.application;

import com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentProviderGateway {

    ProviderPayment createCardPayment(UUID merchantPaymentId, BigDecimal amount);

    ProviderPaymentStatus getPaymentStatus(UUID providerPaymentId);

    record ProviderPayment(UUID providerPaymentId, ProviderPaymentStatus status) {
    }
}
