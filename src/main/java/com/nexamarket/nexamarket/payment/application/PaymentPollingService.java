package com.nexamarket.nexamarket.payment.application;

import com.nexamarket.nexamarket.payment.domain.PaymentStatus;
import com.nexamarket.nexamarket.payment.domain.PaymentTransaction;
import com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus;
import com.nexamarket.nexamarket.payment.infrastructure.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class PaymentPollingService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentProviderGateway paymentProviderGateway;
    private final PaymentWebhookService paymentWebhookService;
    private final Clock clock;
    private final Duration pollingInterval;

    @Autowired
    public PaymentPollingService(PaymentTransactionRepository paymentTransactionRepository,
                                 PaymentProviderGateway paymentProviderGateway,
                                 PaymentWebhookService paymentWebhookService,
                                 @Value("${payment.polling.interval}") Duration pollingInterval) {
        this(paymentTransactionRepository, paymentProviderGateway, paymentWebhookService,
                pollingInterval, Clock.systemUTC());
    }

    PaymentPollingService(PaymentTransactionRepository paymentTransactionRepository,
                          PaymentProviderGateway paymentProviderGateway,
                          PaymentWebhookService paymentWebhookService,
                          Duration pollingInterval, Clock clock) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentProviderGateway = paymentProviderGateway;
        this.paymentWebhookService = paymentWebhookService;
        this.pollingInterval = pollingInterval;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${payment.polling.check-interval-ms}")
    @Transactional
    public int pollDuePaymentsOnSchedule() {
        return pollDuePayments();
    }

    @Transactional
    public int pollDuePayments() {
        Instant now = Instant.now(clock);
        List<PaymentTransaction> duePayments = paymentTransactionRepository.findDueForPollingForUpdate(PaymentStatus.PENDING, now);
        for (PaymentTransaction payment : duePayments) {
            ProviderPaymentStatus providerStatus = paymentProviderGateway.getPaymentStatus(payment.getProviderPaymentId());
            if (providerStatus == ProviderPaymentStatus.PENDING) {
                payment.scheduleNextPoll(now.plus(pollingInterval));
                paymentTransactionRepository.save(payment);
                continue;
            }
            String eventId = "poll-" + payment.getProviderPaymentId() + "-" + providerStatus;
            paymentWebhookService.handle(new PaymentWebhookCommand(eventId, payment.getProviderPaymentId(),
                    providerStatus, "Provider reported a failed payment during polling."));
        }
        return duePayments.size();
    }
}
