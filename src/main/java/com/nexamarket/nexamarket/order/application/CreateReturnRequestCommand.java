package com.nexamarket.nexamarket.order.application;

import java.util.UUID;

public record CreateReturnRequestCommand(UUID subOrderId, String reason) {
}
