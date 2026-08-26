package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSubOrderStatusRequest(@NotNull OrderStatus status) {
}
