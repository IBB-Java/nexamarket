package com.nexamarket.nexamarket.order.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateDeliveryAssignmentRequest(
        @NotNull UUID subOrderId,
        @NotNull @Positive Long courierId
) {
}
