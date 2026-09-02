package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerOrderSummaryResponse(
        UUID orderId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        List<CustomerSubOrderSummaryResponse> subOrders
) {
    public static CustomerOrderSummaryResponse from(CustomerOrder order) {
        return new CustomerOrderSummaryResponse(
                order.getId(), order.getStatus(), order.getTotalAmount(), order.getCreatedAt(),
                order.getSubOrders().stream().map(CustomerSubOrderSummaryResponse::from).toList());
    }
}
