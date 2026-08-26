package com.nexamarket.nexamarket.payment.mock;

import com.nexamarket.nexamarket.payment.application.PaymentWebhookCommand;
import com.nexamarket.nexamarket.payment.application.PaymentWebhookService;
import com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockPaymentProviderServiceTest {

    @Mock
    private MockProviderPaymentRepository paymentRepository;
    @Mock
    private MockProviderCallbackRepository callbackRepository;
    @Mock
    private PaymentWebhookService paymentWebhookService;

    @Test
    void deliversTheSameProviderEventMoreThanOnceWhenConfigured() {
        Instant now = Instant.parse("2026-08-26T09:00:00Z");
        MockProviderPayment providerPayment = MockProviderPayment.create(UUID.randomUUID(), new BigDecimal("30.00"));
        UUID providerPaymentId = providerPayment.getId();
        MockProviderCallback callback = MockProviderCallback.schedule(providerPaymentId, "mock-event-1",
                ProviderPaymentStatus.SUCCEEDED, 2, now);
        MockPaymentProviderService service = new MockPaymentProviderService(paymentRepository, callbackRepository,
                paymentWebhookService, Clock.fixed(now, ZoneOffset.UTC));
        when(callbackRepository.findDueForDeliveryForUpdate(now)).thenReturn(List.of(callback));
        when(paymentRepository.findById(providerPaymentId)).thenReturn(java.util.Optional.of(providerPayment));

        int delivered = service.deliverDueCallbacks();

        assertThat(delivered).isEqualTo(1);
        PaymentWebhookCommand expected = new PaymentWebhookCommand("mock-event-1", providerPaymentId,
                ProviderPaymentStatus.SUCCEEDED, "Mock provider rejected the payment.");
        verify(paymentWebhookService, times(2)).handle(expected);
        verify(callbackRepository).save(callback);
    }
}
