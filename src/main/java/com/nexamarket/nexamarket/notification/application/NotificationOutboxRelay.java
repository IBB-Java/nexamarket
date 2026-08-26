package com.nexamarket.nexamarket.notification.application;

import com.nexamarket.nexamarket.notification.domain.NotificationOutboxEvent;
import com.nexamarket.nexamarket.notification.infrastructure.NotificationMessagingConfiguration;
import com.nexamarket.nexamarket.notification.infrastructure.NotificationOutboxEventRepository;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class NotificationOutboxRelay {
    private final NotificationOutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final Duration retryDelay;
    public NotificationOutboxRelay(NotificationOutboxEventRepository repository, RabbitTemplate rabbitTemplate,
                                   @Value("${notification.outbox.retry-delay}") Duration retryDelay) {
        this.repository = repository; this.rabbitTemplate = rabbitTemplate; this.retryDelay = retryDelay;
    }
    @Scheduled(fixedDelayString = "${notification.outbox.check-interval-ms}") @Transactional
    public int publishDueEvents() {
        Instant now = Instant.now(); List<NotificationOutboxEvent> events = repository.findDueForPublishForUpdate(now);
        for (NotificationOutboxEvent event : events) {
            try { rabbitTemplate.convertAndSend(NotificationMessagingConfiguration.EXCHANGE,
                    NotificationMessagingConfiguration.ORDER_STATUS_ROUTING_KEY, event.asEvent()); event.markPublished(now); }
            catch (AmqpException exception) { event.retryAt(now.plus(retryDelay)); }
            repository.save(event);
        }
        return events.size();
    }
}
