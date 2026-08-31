package com.nexamarket.nexamarket.order.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignCourierRequest(
        @NotNull @Positive Long courierId
) {
}
