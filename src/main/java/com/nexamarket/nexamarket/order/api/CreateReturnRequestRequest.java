package com.nexamarket.nexamarket.order.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateReturnRequestRequest(@NotNull UUID subOrderId, @NotBlank String reason) {
}
