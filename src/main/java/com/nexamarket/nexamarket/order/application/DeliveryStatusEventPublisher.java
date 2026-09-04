package com.nexamarket.nexamarket.order.application;

public interface DeliveryStatusEventPublisher {
    void enqueue(DeliveryStatusChangedEvent event);
}
