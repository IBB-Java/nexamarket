package com.nexamarket.nexamarket.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_messages")
public class NotificationMessage {
    @Id private UUID id;
    @Column(name = "deduplication_key", nullable = false, unique = true) private String deduplicationKey;
    @Column(name = "recipient_id", nullable = false) private UUID recipientId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationChannel channel;
    @Column(nullable = false) private String subject;
    @Column(nullable = false) private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationStatus status;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "sent_at") private Instant sentAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected NotificationMessage() { }
    private NotificationMessage(UUID eventId, UUID recipientId, NotificationChannel channel, String subject, String content, Instant now) {
        this.id = UUID.randomUUID(); this.deduplicationKey = eventId + ":" + channel; this.recipientId = recipientId;
        this.channel = channel; this.subject = subject; this.content = content; this.status = NotificationStatus.PENDING; this.nextAttemptAt = now;
    }
    public static NotificationMessage create(UUID eventId, UUID recipientId, NotificationChannel channel, String subject, String content, Instant now) { return new NotificationMessage(eventId, recipientId, channel, subject, content, now); }
    public void markSent(Instant now) { status = NotificationStatus.SENT; sentAt = now; }
    public void retry(Instant now, Duration delay, int maximumAttempts) { attempts++; if (attempts >= maximumAttempts) { status = NotificationStatus.FAILED; } else { status = NotificationStatus.RETRYING; nextAttemptAt = now.plus(delay); } }
    public NotificationChannel getChannel() { return channel; }
    public NotificationStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
}
