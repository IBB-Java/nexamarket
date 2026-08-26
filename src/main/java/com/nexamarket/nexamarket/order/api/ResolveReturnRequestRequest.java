package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.ReturnRequestStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResolveReturnRequestRequest(@NotNull ReturnRequestStatus status, @NotNull UUID resolverId) {
}
