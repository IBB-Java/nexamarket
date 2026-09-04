package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.DeliveryAssignment;
import com.nexamarket.nexamarket.order.domain.DeliveryAssignmentStatus;
import com.nexamarket.nexamarket.order.domain.DeliveryFailureReasonCode;
import com.nexamarket.nexamarket.order.domain.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminDeliveryAssignmentResponse(
        UUID assignmentId,
        UUID subOrderId,
        UUID orderId,
        Long customerId,
        Long sellerId,
        Long courierId,
        DeliveryAssignmentStatus status,
        OrderStatus orderStatus,
        boolean active,
        Instant assignedAt,
        Instant acceptedAt,
        Instant rejectedAt,
        Instant pickedUpAt,
        Instant deliveryStartedAt,
        Instant deliveredAt,
        Instant failedAt,
        String rejectionReason,
        DeliveryFailureReasonCode failureReasonCode,
        String failureDescription
) {
    public static AdminDeliveryAssignmentResponse from(DeliveryAssignment assignment) {
        var subOrder = assignment.getSubOrder();
        return new AdminDeliveryAssignmentResponse(
                assignment.getId(), subOrder.getId(), subOrder.getOrder().getId(),
                subOrder.getOrder().getCustomerId(), subOrder.getSellerId(), assignment.getCourierId(),
                assignment.getStatus(), subOrder.getStatus(), assignment.isActive(), assignment.getAssignedAt(),
                assignment.getAcceptedAt(), assignment.getRejectedAt(), assignment.getPickedUpAt(),
                assignment.getDeliveryStartedAt(), assignment.getDeliveredAt(), assignment.getFailedAt(),
                assignment.getRejectionReason(), assignment.getFailureReasonCode(), assignment.getFailureDescription());
    }
}
