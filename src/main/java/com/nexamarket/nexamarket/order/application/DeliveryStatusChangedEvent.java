package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.domain.DeliveryAssignmentStatus;

import java.util.UUID;

public record DeliveryStatusChangedEvent(
        UUID id,
        Long recipientId,
        UUID assignmentId,
        UUID subOrderId,
        DeliveryAssignmentStatus status
) {
}
