package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerOrderSummaryResponse(
        UUID orderId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt
) {
    public static CustomerOrderSummaryResponse from(CustomerOrder order) {
        return new CustomerOrderSummaryResponse(
                order.getId(), order.getStatus(), order.getTotalAmount(), order.getCreatedAt());
    }
}
