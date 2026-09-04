package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A role-filtered sub-order row used by the shared order-list API. */
public record RoleOrderResponse(
        UUID orderId,
        UUID subOrderId,
        Long customerId,
        Long sellerId,
        Long courierId,
        OrderStatus orderStatus,
        OrderStatus status,
        BigDecimal orderTotal,
        BigDecimal subtotal,
        int itemCount,
        Instant createdAt
) {

    public static RoleOrderResponse from(SubOrder subOrder, boolean includeCustomerId) {
        int itemCount = subOrder.getItems().stream().mapToInt(item -> item.getQuantity()).sum();
        return new RoleOrderResponse(
                subOrder.getOrder().getId(),
                subOrder.getId(),
                includeCustomerId ? subOrder.getOrder().getCustomerId() : null,
                subOrder.getSellerId(),
                subOrder.getCourierId(),
                subOrder.getOrder().getStatus(),
                subOrder.getStatus(),
                subOrder.getOrder().getTotalAmount(),
                subOrder.getSubtotal(),
                itemCount,
                subOrder.getCreatedAt());
    }
}
