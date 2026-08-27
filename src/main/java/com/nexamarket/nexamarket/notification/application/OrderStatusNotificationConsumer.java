package com.nexamarket.nexamarket.notification.application;

import com.nexamarket.nexamarket.notification.domain.NotificationChannel;
import com.nexamarket.nexamarket.notification.domain.NotificationMessage;
import com.nexamarket.nexamarket.notification.infrastructure.NotificationMessagingConfiguration;
import com.nexamarket.nexamarket.notification.infrastructure.NotificationMessageRepository;
import com.nexamarket.nexamarket.order.application.OrderStatusChangedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OrderStatusNotificationConsumer {
    private final NotificationMessageRepository repository;
    private final AdminOrderWebSocketNotifier adminOrderWebSocketNotifier;

    @Autowired
    public OrderStatusNotificationConsumer(NotificationMessageRepository repository,
                                           AdminOrderWebSocketNotifier adminOrderWebSocketNotifier) {
        this.repository = repository;
        this.adminOrderWebSocketNotifier = adminOrderWebSocketNotifier;
    }

    OrderStatusNotificationConsumer(NotificationMessageRepository repository) {
        this(repository, null);
    }
    @RabbitListener(queues = NotificationMessagingConfiguration.QUEUE)
    @Transactional
    public void receive(OrderStatusChangedEvent event) {
        for (NotificationChannel channel : NotificationChannel.values()) {
            String key = event.id() + ":" + channel;
            if (!repository.existsByDeduplicationKey(key)) {
                String subject = "Sipariş durumun güncellendi";
                String content = "Alt sipariş " + event.subOrderId() + " durumu: " + event.status();
                repository.save(NotificationMessage.create(event.id(), event.recipientId(), channel, subject, content, Instant.now()));
            }
        }
        if (adminOrderWebSocketNotifier != null) {
            adminOrderWebSocketNotifier.publish(event);
        }
    }
}
