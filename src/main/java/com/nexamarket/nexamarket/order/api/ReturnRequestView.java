package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.ReturnRequest;
import com.nexamarket.nexamarket.order.domain.ReturnRequestStatus;

import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;

public record ReturnRequestView(
        UUID id,
        ReturnRequestStatus status,
        String reason,
        UUID orderId,
        UUID subOrderId,
        Long sellerId,
        BigDecimal amount,
        Instant createdAt,
        Instant resolvedAt
) {

    public static ReturnRequestView from(ReturnRequest returnRequest) {
        return new ReturnRequestView(returnRequest.getId(), returnRequest.getStatus(), returnRequest.getReason(),
                returnRequest.getSubOrder().getOrder().getId(), returnRequest.getSubOrder().getId(),
                returnRequest.getSubOrder().getSellerId(), returnRequest.getSubOrder().getSubtotal(),
                returnRequest.getCreatedAt(), returnRequest.getResolvedAt());
    }
}
