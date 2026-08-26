package com.nexamarket.nexamarket.notification.application;

import com.nexamarket.nexamarket.notification.domain.NotificationOutboxEvent;
import com.nexamarket.nexamarket.notification.infrastructure.NotificationOutboxEventRepository;
import com.nexamarket.nexamarket.order.application.OrderStatusChangedEvent;
import com.nexamarket.nexamarket.order.application.OrderStatusEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class NotificationOutboxPublisher implements OrderStatusEventPublisher {
    private final NotificationOutboxEventRepository repository;
    public NotificationOutboxPublisher(NotificationOutboxEventRepository repository) { this.repository = repository; }
    @Override public void enqueue(OrderStatusChangedEvent event) { repository.save(NotificationOutboxEvent.from(event, Instant.now())); }
}
