package com.nexamarket.nexamarket.notification.application;

import com.nexamarket.nexamarket.notification.domain.DeliveryNotificationOutboxEvent;
import com.nexamarket.nexamarket.notification.infrastructure.DeliveryNotificationOutboxEventRepository;
import com.nexamarket.nexamarket.notification.infrastructure.NotificationMessagingConfiguration;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class DeliveryNotificationOutboxRelay {

    private final DeliveryNotificationOutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final Duration retryDelay;

    public DeliveryNotificationOutboxRelay(DeliveryNotificationOutboxEventRepository repository,
                                           RabbitTemplate rabbitTemplate,
                                           @Value("${notification.outbox.retry-delay}") Duration retryDelay) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.retryDelay = retryDelay;
    }

    @Scheduled(fixedDelayString = "${notification.outbox.check-interval-ms}")
    @Transactional
    public int publishDueEvents() {
        Instant now = Instant.now();
        var events = repository.findDueForPublishForUpdate(now);
        for (DeliveryNotificationOutboxEvent event : events) {
            try {
                rabbitTemplate.convertAndSend(NotificationMessagingConfiguration.EXCHANGE,
                        NotificationMessagingConfiguration.DELIVERY_STATUS_ROUTING_KEY, event.asEvent());
                event.markPublished(now);
            } catch (AmqpException exception) {
                event.retryAt(now.plus(retryDelay));
            }
            repository.save(event);
        }
        return events.size();
    }
}
