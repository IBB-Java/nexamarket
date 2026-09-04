package com.nexamarket.nexamarket.notification.application;

import com.nexamarket.nexamarket.notification.domain.DeliveryNotificationOutboxEvent;
import com.nexamarket.nexamarket.notification.infrastructure.DeliveryNotificationOutboxEventRepository;
import com.nexamarket.nexamarket.order.application.DeliveryStatusChangedEvent;
import com.nexamarket.nexamarket.order.application.DeliveryStatusEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeliveryNotificationOutboxPublisher implements DeliveryStatusEventPublisher {

    private final DeliveryNotificationOutboxEventRepository repository;

    @Override
    public void enqueue(DeliveryStatusChangedEvent event) {
        repository.save(DeliveryNotificationOutboxEvent.from(event, Instant.now()));
    }
}
