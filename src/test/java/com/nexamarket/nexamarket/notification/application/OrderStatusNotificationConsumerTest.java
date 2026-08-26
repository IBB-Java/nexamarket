package com.nexamarket.nexamarket.notification.application;

import com.nexamarket.nexamarket.notification.infrastructure.NotificationMessageRepository;
import com.nexamarket.nexamarket.order.application.OrderStatusChangedEvent;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusNotificationConsumerTest {
    @Mock private NotificationMessageRepository repository;
    @Test void createsOneMessagePerChannelForANewEvent() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), OrderStatus.SHIPPED);
        when(repository.existsByDeduplicationKey(any())).thenReturn(false);
        new OrderStatusNotificationConsumer(repository).receive(event);
        verify(repository, times(3)).save(any());
    }
}
