package com.nexamarket.nexamarket.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "wallet_accounts")
public class WalletAccount {

    @Id
    @Column(name = "customer_id")
    private UUID customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Version
    @Column(nullable = false)
    private long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WalletAccount() {
    }

    private WalletAccount(UUID customerId, BigDecimal openingBalance) {
        this.customerId = Objects.requireNonNull(customerId, "Customer id is required.");
        this.balance = requireNonNegative(openingBalance, "Opening balance must be non-negative.");
    }

    public static WalletAccount open(UUID customerId, BigDecimal openingBalance) {
        return new WalletAccount(customerId, openingBalance);
    }

    public void debit(BigDecimal amount) {
        BigDecimal debitAmount = requireNonNegative(amount, "Debit amount must be non-negative.");
        if (balance.compareTo(debitAmount) < 0) {
            throw new IllegalStateException("Wallet balance is insufficient.");
        }
        balance = balance.subtract(debitAmount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(requireNonNegative(amount, "Credit amount must be non-negative."));
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    private static BigDecimal requireNonNegative(BigDecimal amount, String message) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException(message);
        }
        return amount;
    }
}
