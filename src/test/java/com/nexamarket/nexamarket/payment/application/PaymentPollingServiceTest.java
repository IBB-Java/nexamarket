package com.nexamarket.nexamarket.payment.application;

import com.nexamarket.nexamarket.payment.domain.PaymentStatus;
import com.nexamarket.nexamarket.payment.domain.PaymentTransaction;
import com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus;
import com.nexamarket.nexamarket.payment.infrastructure.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentPollingServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private PaymentProviderGateway paymentProviderGateway;
    @Mock
    private PaymentWebhookService paymentWebhookService;

    @Test
    void reschedulesPendingProviderPaymentsForRetry() {
        Instant now = Instant.parse("2026-08-26T09:00:00Z");
        PaymentTransaction payment = PaymentTransaction.initiate(UUID.randomUUID(), 511L, "poll-key",
                BigDecimal.ZERO, new BigDecimal("30.00"), now);
        payment.assignProviderPayment(UUID.randomUUID());
        PaymentPollingService service = new PaymentPollingService(paymentTransactionRepository, paymentProviderGateway,
                paymentWebhookService, Duration.ofSeconds(30), Clock.fixed(now, ZoneOffset.UTC));

        when(paymentTransactionRepository.findDueForPollingForUpdate(PaymentStatus.PENDING, now))
                .thenReturn(List.of(payment));
        when(paymentProviderGateway.getPaymentStatus(payment.getProviderPaymentId()))
                .thenReturn(ProviderPaymentStatus.PENDING);

        int polled = service.pollDuePayments();

        ArgumentCaptor<PaymentTransaction> paymentCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        assertThat(polled).isEqualTo(1);
        assertThat(paymentCaptor.getValue().getPollingAttempts()).isEqualTo(1);
        assertThat(paymentCaptor.getValue().getNextPollAt()).isEqualTo(now.plusSeconds(30));
    }
}
