package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.domain.OrderStatus;

import java.util.UUID;

public record OrderStatusChangedEvent(UUID id, Long recipientId, UUID subOrderId,
                                      Long sellerId, OrderStatus status) {
}
