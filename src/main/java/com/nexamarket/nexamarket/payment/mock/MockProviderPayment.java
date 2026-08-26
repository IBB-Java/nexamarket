package com.nexamarket.nexamarket.payment.mock;

import com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "mock_provider_payments")
public class MockProviderPayment {

    @Id
    private UUID id;

    @Column(name = "merchant_payment_id", nullable = false, unique = true, updatable = false)
    private UUID merchantPaymentId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProviderPaymentStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MockProviderPayment() {
    }

    private MockProviderPayment(UUID merchantPaymentId, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.merchantPaymentId = Objects.requireNonNull(merchantPaymentId, "Merchant payment id is required.");
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Provider payment amount must be positive.");
        }
        this.amount = amount;
        this.status = ProviderPaymentStatus.PENDING;
    }

    public static MockProviderPayment create(UUID merchantPaymentId, BigDecimal amount) {
        return new MockProviderPayment(merchantPaymentId, amount);
    }

    public void setOutcome(ProviderPaymentStatus status, String failureReason) {
        if (status == null || status == ProviderPaymentStatus.PENDING) {
            throw new IllegalArgumentException("A provider outcome must be succeeded or failed.");
        }
        if (this.status != ProviderPaymentStatus.PENDING) {
            throw new IllegalStateException("Provider payment already has a final outcome.");
        }
        this.status = status;
        this.failureReason = status == ProviderPaymentStatus.FAILED ? failureReason : null;
    }

    public UUID getId() {
        return id;
    }

    public ProviderPaymentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
