package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerOrderSummaryResponse(
        UUID orderId,
        Long customerId,
        String customerEmail,
        String customerName,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        List<CustomerSubOrderSummaryResponse> subOrders
) {
    public static CustomerOrderSummaryResponse from(CustomerOrder order) {
        return from(order, order.getSubOrders(), null);
    }

    public static CustomerOrderSummaryResponse from(CustomerOrder order, List<SubOrder> visibleSubOrders) {
        return from(order, visibleSubOrders, null);
    }

    public static CustomerOrderSummaryResponse from(CustomerOrder order, List<SubOrder> visibleSubOrders,
                                                    String customerName) {
        BigDecimal visibleTotal = visibleSubOrders.size() == order.getSubOrders().size()
                ? order.getTotalAmount()
                : visibleSubOrders.stream().map(SubOrder::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CustomerOrderSummaryResponse(
                order.getId(), order.getCustomerId(), order.getCustomerEmail(), customerName, order.getStatus(), visibleTotal,
                order.getCreatedAt(), visibleSubOrders.stream().map(CustomerSubOrderSummaryResponse::from).toList());
    }
}
