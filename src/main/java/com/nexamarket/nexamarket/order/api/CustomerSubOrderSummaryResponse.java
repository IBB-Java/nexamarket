package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerSubOrderSummaryResponse(
        UUID subOrderId,
        Long sellerId,
        OrderStatus status,
        BigDecimal subtotal,
        int itemCount
) {
    public static CustomerSubOrderSummaryResponse from(SubOrder subOrder) {
        int itemCount = subOrder.getItems().stream().mapToInt(item -> item.getQuantity()).sum();
        return new CustomerSubOrderSummaryResponse(subOrder.getId(), subOrder.getSellerId(), subOrder.getStatus(),
                subOrder.getSubtotal(), itemCount);
    }
}
