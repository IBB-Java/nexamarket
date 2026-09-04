package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.DeliveryAssignment;
import com.nexamarket.nexamarket.order.domain.DeliveryAssignmentStatus;
import com.nexamarket.nexamarket.order.domain.DeliveryFailureReasonCode;
import com.nexamarket.nexamarket.order.domain.OrderStatus;

import java.time.Instant;
import java.util.UUID;

/** Kurye için yalnızca teslimatı yürütmekte gerekli alanları taşır. */
public record CourierDeliveryResponse(
        UUID assignmentId,
        UUID subOrderId,
        UUID orderId,
        DeliveryAssignmentStatus status,
        OrderStatus orderStatus,
        int packageCount,
        Instant assignedAt,
        Instant acceptedAt,
        Instant pickedUpAt,
        Instant deliveryStartedAt,
        Instant deliveredAt,
        Instant rejectedAt,
        Instant failedAt,
        String rejectionReason,
        DeliveryFailureReasonCode failureReasonCode,
        String failureDescription
) {
    public static CourierDeliveryResponse from(DeliveryAssignment assignment) {
        var subOrder = assignment.getSubOrder();
        int packageCount = subOrder.getItems().stream().mapToInt(item -> item.getQuantity()).sum();
        return new CourierDeliveryResponse(
                assignment.getId(), subOrder.getId(), subOrder.getOrder().getId(), assignment.getStatus(),
                subOrder.getStatus(), packageCount, assignment.getAssignedAt(), assignment.getAcceptedAt(),
                assignment.getPickedUpAt(), assignment.getDeliveryStartedAt(), assignment.getDeliveredAt(),
                assignment.getRejectedAt(), assignment.getFailedAt(), assignment.getRejectionReason(),
                assignment.getFailureReasonCode(), assignment.getFailureDescription());
    }
}
