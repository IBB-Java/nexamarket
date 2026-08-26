package com.nexamarket.nexamarket.notification.application;

import com.nexamarket.nexamarket.notification.domain.NotificationChannel;
import com.nexamarket.nexamarket.notification.domain.NotificationMessage;
import com.nexamarket.nexamarket.notification.domain.NotificationStatus;
import com.nexamarket.nexamarket.notification.infrastructure.NotificationMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationDeliveryService {
    private final NotificationMessageRepository repository;
    private final Map<NotificationChannel, NotificationChannelSender> senders;
    private final Duration retryDelay;
    private final int maximumAttempts;
    public NotificationDeliveryService(NotificationMessageRepository repository, List<NotificationChannelSender> senders,
                                       @Value("${notification.retry-delay}") Duration retryDelay,
                                       @Value("${notification.maximum-attempts}") int maximumAttempts) {
        this.repository = repository; this.retryDelay = retryDelay; this.maximumAttempts = maximumAttempts;
        this.senders = new EnumMap<>(NotificationChannel.class);
        senders.forEach(sender -> this.senders.put(sender.channel(), sender));
    }
    @Scheduled(fixedDelayString = "${notification.delivery.check-interval-ms}") @Transactional
    public int deliverDueNotifications() {
        Instant now = Instant.now(); List<NotificationMessage> messages = repository.findDueForDeliveryForUpdate(List.of(NotificationStatus.PENDING, NotificationStatus.RETRYING), now);
        for (NotificationMessage message : messages) {
            try { senders.get(message.getChannel()).send(message); message.markSent(now); }
            catch (RuntimeException exception) { message.retry(now, retryDelay, maximumAttempts); }
            repository.save(message);
        }
        return messages.size();
    }
}
