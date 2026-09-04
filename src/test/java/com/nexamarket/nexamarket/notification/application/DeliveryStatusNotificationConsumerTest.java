package com.nexamarket.nexamarket.notification.application;

import com.nexamarket.nexamarket.notification.infrastructure.NotificationMessageRepository;
import com.nexamarket.nexamarket.order.application.DeliveryStatusChangedEvent;
import com.nexamarket.nexamarket.order.domain.DeliveryAssignmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryStatusNotificationConsumerTest {

    @Mock
    private NotificationMessageRepository repository;

    @Test
    void createsOneMessagePerChannelForNewDeliveryEvent() {
        DeliveryStatusChangedEvent event = event();
        when(repository.existsByDeduplicationKey(any())).thenReturn(false);

        new DeliveryStatusNotificationConsumer(repository).receive(event);

        verify(repository, times(3)).save(any());
    }

    @Test
    void duplicateDeliveryEventDoesNotCreateMessages() {
        DeliveryStatusChangedEvent event = event();
        when(repository.existsByDeduplicationKey(any())).thenReturn(true);

        new DeliveryStatusNotificationConsumer(repository).receive(event);

        verify(repository, never()).save(any());
    }

    private DeliveryStatusChangedEvent event() {
        return new DeliveryStatusChangedEvent(
                UUID.randomUUID(), 601L, UUID.randomUUID(), UUID.randomUUID(),
                DeliveryAssignmentStatus.IN_TRANSIT);
    }
}
