package com.nexamarket.nexamarket.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "return_requests")
public class ReturnRequest {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_order_id", nullable = false, unique = true, updatable = false)
    private SubOrder subOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnRequestStatus status;

    @Column(nullable = false, length = 1000, updatable = false)
    private String reason;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected ReturnRequest() {
    }

    public ReturnRequest(SubOrder subOrder, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Return reason is required.");
        }
        this.id = UUID.randomUUID();
        this.subOrder = Objects.requireNonNull(subOrder, "Sub-order is required.");
        this.status = ReturnRequestStatus.REQUESTED;
        this.reason = reason.trim();
    }

    public void resolve(ReturnRequestStatus targetStatus, UUID resolverId, Instant resolvedAt) {
        if (status != ReturnRequestStatus.REQUESTED) {
            throw new IllegalStateException("Only a requested return can be resolved.");
        }
        if (targetStatus == ReturnRequestStatus.REQUESTED) {
            throw new IllegalArgumentException("Return resolution must be approved or rejected.");
        }
        this.status = targetStatus;
        this.resolvedBy = Objects.requireNonNull(resolverId, "Resolver id is required.");
        this.resolvedAt = Objects.requireNonNull(resolvedAt, "Resolution time is required.");
    }

    public UUID getId() {
        return id;
    }

    public SubOrder getSubOrder() {
        return subOrder;
    }

    public ReturnRequestStatus getStatus() {
        return status;
    }
}
