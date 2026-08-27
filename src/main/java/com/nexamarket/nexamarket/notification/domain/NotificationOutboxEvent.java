package com.nexamarket.nexamarket.notification.domain;

import com.nexamarket.nexamarket.order.application.OrderStatusChangedEvent;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
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
@Table(name = "notification_outbox_events")
public class NotificationOutboxEvent {
    @Id private UUID id;
    @Column(name = "recipient_id", nullable = false) private Long recipientId;
    @Column(name = "sub_order_id", nullable = false) private UUID subOrderId;
    @Column(name = "seller_id", nullable = false) private Long sellerId;
    @Enumerated(EnumType.STRING) @Column(name = "order_status", nullable = false) private OrderStatus orderStatus;
    @Column(name = "publish_attempts", nullable = false) private int publishAttempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "published_at") private Instant publishedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected NotificationOutboxEvent() { }
    private NotificationOutboxEvent(OrderStatusChangedEvent event, Instant now) {
        this.id = event.id(); this.recipientId = event.recipientId(); this.subOrderId = event.subOrderId();
        this.sellerId = event.sellerId(); this.orderStatus = event.status(); this.nextAttemptAt = now;
    }
    public static NotificationOutboxEvent from(OrderStatusChangedEvent event, Instant now) { return new NotificationOutboxEvent(event, now); }
    public void markPublished(Instant now) { publishedAt = now; }
    public void retryAt(Instant nextAttemptAt) { publishAttempts++; this.nextAttemptAt = nextAttemptAt; }
    public OrderStatusChangedEvent asEvent() { return new OrderStatusChangedEvent(id, recipientId, subOrderId, sellerId, orderStatus); }
}
