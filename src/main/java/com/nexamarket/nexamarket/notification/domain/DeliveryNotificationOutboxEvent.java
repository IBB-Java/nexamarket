package com.nexamarket.nexamarket.notification.domain;

import com.nexamarket.nexamarket.order.application.DeliveryStatusChangedEvent;
import com.nexamarket.nexamarket.order.domain.DeliveryAssignmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_notification_outbox_events")
public class DeliveryNotificationOutboxEvent {
    @Id private UUID id;
    @Column(name = "recipient_id", nullable = false) private Long recipientId;
    @Column(name = "assignment_id", nullable = false) private UUID assignmentId;
    @Column(name = "sub_order_id", nullable = false) private UUID subOrderId;
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false) private DeliveryAssignmentStatus deliveryStatus;
    @Column(name = "publish_attempts", nullable = false) private int publishAttempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "published_at") private Instant publishedAt;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected DeliveryNotificationOutboxEvent() {
    }

    private DeliveryNotificationOutboxEvent(DeliveryStatusChangedEvent event, Instant now) {
        this.id = event.id();
        this.recipientId = event.recipientId();
        this.assignmentId = event.assignmentId();
        this.subOrderId = event.subOrderId();
        this.deliveryStatus = event.status();
        this.nextAttemptAt = now;
    }

    public static DeliveryNotificationOutboxEvent from(DeliveryStatusChangedEvent event, Instant now) {
        return new DeliveryNotificationOutboxEvent(event, now);
    }

    public void markPublished(Instant now) { publishedAt = now; }
    public void retryAt(Instant nextAttemptAt) { publishAttempts++; this.nextAttemptAt = nextAttemptAt; }
    public DeliveryStatusChangedEvent asEvent() {
        return new DeliveryStatusChangedEvent(id, recipientId, assignmentId, subOrderId, deliveryStatus);
    }
}
