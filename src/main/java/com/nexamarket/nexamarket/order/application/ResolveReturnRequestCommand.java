package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.domain.ReturnRequestStatus;

import java.util.UUID;

public record ResolveReturnRequestCommand(UUID returnRequestId, ReturnRequestStatus status, Long resolverId) {
}
