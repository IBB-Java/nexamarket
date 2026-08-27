package com.nexamarket.loyalty.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Immutable accounting line; the point balance is the sum of this ledger. */
@Entity
@Table(name = "loyalty_ledger_entries")
@Getter
@NoArgsConstructor
public class LoyaltyLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private Long customerId;

    @Column(name = "sub_order_id", nullable = false, updatable = false)
    private UUID subOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private LoyaltyEntryType type;

    @Column(nullable = false, updatable = false)
    private int points;

    @Column(nullable = false, length = 250, updatable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LoyaltyLedgerEntry(Long customerId, UUID subOrderId, LoyaltyEntryType type, int points,
                              String description, Instant createdAt) {
        this.customerId = customerId;
        this.subOrderId = subOrderId;
        this.type = type;
        this.points = points;
        this.description = description;
        this.createdAt = createdAt;
    }
}
