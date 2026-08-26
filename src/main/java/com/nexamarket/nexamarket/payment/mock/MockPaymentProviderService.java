package com.nexamarket.nexamarket.payment.mock;

import com.nexamarket.nexamarket.payment.application.PaymentProviderGateway;
import com.nexamarket.nexamarket.payment.application.PaymentWebhookCommand;
import com.nexamarket.nexamarket.payment.application.PaymentWebhookService;
import com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A deterministic, local payment-provider substitute. Its explicit outcome
 * endpoint lets us test delayed and duplicated webhooks without real money.
 */
@Service
public class MockPaymentProviderService implements PaymentProviderGateway {

    private final MockProviderPaymentRepository paymentRepository;
    private final MockProviderCallbackRepository callbackRepository;
    private final PaymentWebhookService paymentWebhookService;
    private final Clock clock;

    @Autowired
    public MockPaymentProviderService(MockProviderPaymentRepository paymentRepository,
                                      MockProviderCallbackRepository callbackRepository,
                                      PaymentWebhookService paymentWebhookService) {
        this(paymentRepository, callbackRepository, paymentWebhookService, Clock.systemUTC());
    }

    MockPaymentProviderService(MockProviderPaymentRepository paymentRepository,
                               MockProviderCallbackRepository callbackRepository,
                               PaymentWebhookService paymentWebhookService, Clock clock) {
        this.paymentRepository = paymentRepository;
        this.callbackRepository = callbackRepository;
        this.paymentWebhookService = paymentWebhookService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProviderPayment createCardPayment(UUID merchantPaymentId, BigDecimal amount) {
        MockProviderPayment payment = paymentRepository.save(MockProviderPayment.create(merchantPaymentId, amount));
        return new ProviderPayment(payment.getId(), payment.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderPaymentStatus getPaymentStatus(UUID providerPaymentId) {
        return paymentRepository.findById(providerPaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Mock provider payment was not found."))
                .getStatus();
    }

    @Transactional
    public void setOutcomeAndScheduleCallback(UUID providerPaymentId, ProviderPaymentStatus status,
                                              String failureReason, Duration delay, int duplicateDeliveries) {
        if (status == null || status == ProviderPaymentStatus.PENDING || delay == null || delay.isNegative()
                || duplicateDeliveries < 1 || duplicateDeliveries > 10) {
            throw new IllegalArgumentException("Callback delay or duplicate delivery count is invalid.");
        }
        MockProviderPayment payment = paymentRepository.findById(providerPaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Mock provider payment was not found."));
        if (payment.getStatus() != ProviderPaymentStatus.PENDING
                || callbackRepository.existsByProviderPaymentIdAndDeliveredAtIsNull(providerPaymentId)) {
            throw new IllegalStateException("Provider payment already has an outcome scheduled or delivered.");
        }
        Instant now = Instant.now(clock);
        MockProviderCallback callback = MockProviderCallback.schedule(providerPaymentId, UUID.randomUUID().toString(),
                status, duplicateDeliveries, now.plus(delay));
        callbackRepository.save(callback);
    }

    @Scheduled(fixedDelayString = "${payment.mock-provider.callback.check-interval-ms}")
    @Transactional
    public int deliverDueCallbacksOnSchedule() {
        return deliverDueCallbacks();
    }

    @Transactional
    public int deliverDueCallbacks() {
        Instant now = Instant.now(clock);
        List<MockProviderCallback> callbacks = callbackRepository.findDueForDeliveryForUpdate(now);
        for (MockProviderCallback callback : callbacks) {
            MockProviderPayment payment = paymentRepository.findById(callback.getProviderPaymentId())
                    .orElseThrow(() -> new IllegalStateException("Mock provider payment was not found."));
            payment.setOutcome(callback.getStatus(), "Mock provider rejected the payment.");
            paymentRepository.save(payment);
            for (int delivery = 0; delivery < callback.getDeliveryCount(); delivery++) {
                paymentWebhookService.handle(new PaymentWebhookCommand(callback.getProviderEventId(),
                        callback.getProviderPaymentId(), callback.getStatus(), "Mock provider rejected the payment."));
            }
            callback.markDelivered(now);
            callbackRepository.save(callback);
        }
        return callbacks.size();
    }
}
