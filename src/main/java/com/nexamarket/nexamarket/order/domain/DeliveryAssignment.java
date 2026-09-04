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
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "delivery_assignments")
public class DeliveryAssignment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_order_id", nullable = false, updatable = false)
    private SubOrder subOrder;

    @Column(name = "courier_id", nullable = false, updatable = false)
    private Long courierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryAssignmentStatus status;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "delivery_started_at")
    private Instant deliveryStartedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason_code", length = 40)
    private DeliveryFailureReasonCode failureReasonCode;

    @Column(name = "failure_description", length = 1000)
    private String failureDescription;

    /** TRUE only for the single active assignment; terminal rows use NULL so history remains unique-safe. */
    @Column(name = "active_assignment")
    private Boolean activeAssignment;

    @Version
    @Column(nullable = false)
    private long version;

    protected DeliveryAssignment() {
    }

    private DeliveryAssignment(SubOrder subOrder, Long courierId, Instant assignedAt) {
        this.id = UUID.randomUUID();
        this.subOrder = Objects.requireNonNull(subOrder, "Sub-order is required.");
        this.courierId = Objects.requireNonNull(courierId, "Courier id is required.");
        this.status = DeliveryAssignmentStatus.ASSIGNED;
        this.assignedAt = Objects.requireNonNull(assignedAt, "Assignment time is required.");
        this.activeAssignment = Boolean.TRUE;
    }

    public static DeliveryAssignment assign(SubOrder subOrder, Long courierId, Instant assignedAt) {
        return new DeliveryAssignment(subOrder, courierId, assignedAt);
    }

    void applyTransition(DeliveryAssignmentStatus target, Instant now,
                         DeliveryFailureReasonCode reasonCode, String detail) {
        this.status = target;
        switch (target) {
            case ACCEPTED -> acceptedAt = now;
            case REJECTED -> {
                rejectedAt = now;
                rejectionReason = detail;
                activeAssignment = null;
            }
            case PICKED_UP -> pickedUpAt = now;
            case IN_TRANSIT -> deliveryStartedAt = now;
            case DELIVERED -> {
                deliveredAt = now;
                activeAssignment = null;
            }
            case DELIVERY_FAILED -> {
                failedAt = now;
                failureReasonCode = reasonCode;
                failureDescription = detail;
                activeAssignment = null;
            }
            default -> throw new IllegalArgumentException("ASSIGNED is only valid when creating an assignment.");
        }
    }

    public UUID getId() { return id; }
    public SubOrder getSubOrder() { return subOrder; }
    public Long getCourierId() { return courierId; }
    public DeliveryAssignmentStatus getStatus() { return status; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getRejectedAt() { return rejectedAt; }
    public Instant getPickedUpAt() { return pickedUpAt; }
    public Instant getDeliveryStartedAt() { return deliveryStartedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Instant getFailedAt() { return failedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public DeliveryFailureReasonCode getFailureReasonCode() { return failureReasonCode; }
    public String getFailureDescription() { return failureDescription; }
    public boolean isActive() { return Boolean.TRUE.equals(activeAssignment); }
}
