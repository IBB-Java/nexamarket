package com.nexamarket.nexamarket.payment.domain;

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
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "wallet_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal walletAmount;

    @Column(name = "card_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal cardAmount;

    @Column(name = "provider_payment_id", unique = true)
    private UUID providerPaymentId;

    @Column(name = "polling_attempts", nullable = false)
    private int pollingAttempts;

    @Column(name = "next_poll_at")
    private Instant nextPollAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentTransaction() {
    }

    private PaymentTransaction(UUID orderId, UUID customerId, String idempotencyKey,
                               BigDecimal walletAmount, BigDecimal cardAmount, Instant nextPollAt) {
        this.id = UUID.randomUUID();
        this.orderId = Objects.requireNonNull(orderId, "Order id is required.");
        this.customerId = Objects.requireNonNull(customerId, "Customer id is required.");
        this.idempotencyKey = requireText(idempotencyKey, "Idempotency key is required.");
        this.walletAmount = requireNonNegative(walletAmount, "Wallet amount must be non-negative.");
        this.cardAmount = requireNonNegative(cardAmount, "Card amount must be non-negative.");
        if (this.walletAmount.signum() == 0 && this.cardAmount.signum() == 0) {
            throw new IllegalArgumentException("At least one payment amount must be positive.");
        }
        this.status = PaymentStatus.PENDING;
        this.nextPollAt = nextPollAt;
    }

    public static PaymentTransaction initiate(UUID orderId, UUID customerId, String idempotencyKey,
                                              BigDecimal walletAmount, BigDecimal cardAmount, Instant nextPollAt) {
        return new PaymentTransaction(orderId, customerId, idempotencyKey, walletAmount, cardAmount, nextPollAt);
    }

    public void assignProviderPayment(UUID providerPaymentId) {
        if (cardAmount.signum() == 0) {
            throw new IllegalStateException("A wallet-only payment does not use a provider payment.");
        }
        if (this.providerPaymentId != null) {
            throw new IllegalStateException("Provider payment has already been assigned.");
        }
        this.providerPaymentId = Objects.requireNonNull(providerPaymentId, "Provider payment id is required.");
    }

    public void markSucceeded() {
        if (status == PaymentStatus.FAILED) {
            throw new IllegalStateException("A failed payment cannot be completed.");
        }
        status = PaymentStatus.SUCCEEDED;
        nextPollAt = null;
        failureReason = null;
    }

    public void markFailed(String reason) {
        if (status == PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("A successful payment cannot fail.");
        }
        status = PaymentStatus.FAILED;
        nextPollAt = null;
        failureReason = requireText(reason, "Failure reason is required.");
    }

    public void scheduleNextPoll(Instant nextPollAt) {
        if (status == PaymentStatus.PENDING && providerPaymentId != null) {
            pollingAttempts++;
            this.nextPollAt = Objects.requireNonNull(nextPollAt, "Next poll time is required.");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getWalletAmount() {
        return walletAmount;
    }

    public BigDecimal getCardAmount() {
        return cardAmount;
    }

    public UUID getProviderPaymentId() {
        return providerPaymentId;
    }

    public int getPollingAttempts() {
        return pollingAttempts;
    }

    public Instant getNextPollAt() {
        return nextPollAt;
    }

    private static BigDecimal requireNonNegative(BigDecimal amount, String message) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException(message);
        }
        return amount;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
