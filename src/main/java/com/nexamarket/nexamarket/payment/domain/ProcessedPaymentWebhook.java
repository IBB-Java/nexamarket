package com.nexamarket.nexamarket.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "processed_payment_webhooks")
public class ProcessedPaymentWebhook {

    @Id
    private UUID id;

    @Column(name = "provider_event_id", nullable = false, unique = true, updatable = false, length = 100)
    private String providerEventId;

    @Column(name = "provider_payment_id", nullable = false, updatable = false)
    private UUID providerPaymentId;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected ProcessedPaymentWebhook() {
    }

    private ProcessedPaymentWebhook(String providerEventId, UUID providerPaymentId) {
        this.id = UUID.randomUUID();
        this.providerEventId = requireText(providerEventId);
        this.providerPaymentId = Objects.requireNonNull(providerPaymentId, "Provider payment id is required.");
    }

    public static ProcessedPaymentWebhook received(String providerEventId, UUID providerPaymentId) {
        return new ProcessedPaymentWebhook(providerEventId, providerPaymentId);
    }

    public UUID getId() {
        return id;
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Provider event id is required.");
        }
        return value;
    }
}
