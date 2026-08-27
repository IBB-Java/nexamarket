package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.ReturnRequestStatus;
import jakarta.validation.constraints.NotNull;

public record ResolveReturnRequestRequest(@NotNull ReturnRequestStatus status) {
}
