package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourierOrderResponse(
        UUID subOrderId,
        Long sellerId,
        Long courierId,
        OrderStatus status,
        BigDecimal subtotal,
        Instant createdAt
) {
    public static CourierOrderResponse from(SubOrder subOrder) {
        return new CourierOrderResponse(subOrder.getId(), subOrder.getSellerId(), subOrder.getCourierId(),
                subOrder.getStatus(), subOrder.getSubtotal(), subOrder.getCreatedAt());
    }
}
