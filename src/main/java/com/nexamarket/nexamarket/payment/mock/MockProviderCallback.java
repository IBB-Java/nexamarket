package com.nexamarket.nexamarket.payment.mock;

import com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "mock_provider_callbacks")
public class MockProviderCallback {

    @Id
    private UUID id;

    @Column(name = "provider_payment_id", nullable = false, updatable = false)
    private UUID providerPaymentId;

    @Column(name = "provider_event_id", nullable = false, unique = true, updatable = false, length = 100)
    private String providerEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProviderPaymentStatus status;

    @Column(name = "delivery_count", nullable = false)
    private int deliveryCount;

    @Column(name = "deliver_at", nullable = false)
    private Instant deliverAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected MockProviderCallback() {
    }

    private MockProviderCallback(UUID providerPaymentId, String providerEventId, ProviderPaymentStatus status,
                                 int deliveryCount, Instant deliverAt) {
        this.id = UUID.randomUUID();
        this.providerPaymentId = Objects.requireNonNull(providerPaymentId, "Provider payment id is required.");
        if (providerEventId == null || providerEventId.isBlank()) {
            throw new IllegalArgumentException("Provider event id is required.");
        }
        if (status == null || status == ProviderPaymentStatus.PENDING) {
            throw new IllegalArgumentException("Callback status must be final.");
        }
        if (deliveryCount < 1) {
            throw new IllegalArgumentException("Delivery count must be at least one.");
        }
        this.providerEventId = providerEventId;
        this.status = status;
        this.deliveryCount = deliveryCount;
        this.deliverAt = Objects.requireNonNull(deliverAt, "Delivery time is required.");
    }

    public static MockProviderCallback schedule(UUID providerPaymentId, String providerEventId,
                                                ProviderPaymentStatus status, int deliveryCount, Instant deliverAt) {
        return new MockProviderCallback(providerPaymentId, providerEventId, status, deliveryCount, deliverAt);
    }

    public void markDelivered(Instant deliveredAt) {
        this.deliveredAt = Objects.requireNonNull(deliveredAt, "Delivery time is required.");
    }

    public UUID getProviderPaymentId() {
        return providerPaymentId;
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    public ProviderPaymentStatus getStatus() {
        return status;
    }

    public int getDeliveryCount() {
        return deliveryCount;
    }
}
