package com.nexamarket.nexamarket.notification.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageTest {
    @Test void retriesThenFailsAfterConfiguredAttemptLimit() {
        Instant now = Instant.parse("2026-08-26T09:00:00Z");
        NotificationMessage message = NotificationMessage.create(UUID.randomUUID(), 611L, NotificationChannel.EMAIL, "Durum", "Kargon yolda", now);
        message.retry(now, Duration.ofSeconds(30), 2);
        assertThat(message.getStatus()).isEqualTo(NotificationStatus.RETRYING);
        assertThat(message.getNextAttemptAt()).isEqualTo(now.plusSeconds(30));
        message.retry(now.plusSeconds(30), Duration.ofSeconds(30), 2);
        assertThat(message.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(message.getAttempts()).isEqualTo(2);
    }
}
