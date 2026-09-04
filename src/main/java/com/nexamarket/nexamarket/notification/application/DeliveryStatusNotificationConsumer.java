package com.nexamarket.nexamarket.notification.application;

import com.nexamarket.nexamarket.notification.domain.NotificationChannel;
import com.nexamarket.nexamarket.notification.domain.NotificationMessage;
import com.nexamarket.nexamarket.notification.infrastructure.NotificationMessageRepository;
import com.nexamarket.nexamarket.notification.infrastructure.NotificationMessagingConfiguration;
import com.nexamarket.nexamarket.order.application.DeliveryStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeliveryStatusNotificationConsumer {

    private final NotificationMessageRepository repository;

    @RabbitListener(queues = NotificationMessagingConfiguration.DELIVERY_QUEUE)
    @Transactional
    public void receive(DeliveryStatusChangedEvent event) {
        for (NotificationChannel channel : NotificationChannel.values()) {
            String key = event.id() + ":" + channel;
            if (!repository.existsByDeduplicationKey(key)) {
                String subject = "Teslimat durumun güncellendi";
                String content = "Alt sipariş " + event.subOrderId() + " teslimat durumu: " + event.status();
                repository.save(NotificationMessage.create(
                        event.id(), event.recipientId(), channel, subject, content, Instant.now()));
            }
        }
    }
}
