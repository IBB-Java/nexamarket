package com.nexamarket.nexamarket.order.application;

public interface OrderStatusEventPublisher {

    void enqueue(OrderStatusChangedEvent event);
}
