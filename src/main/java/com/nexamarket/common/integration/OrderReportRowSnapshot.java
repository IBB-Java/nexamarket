package com.nexamarket.common.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderReportRowSnapshot(UUID subOrderId, Long sellerId, String status,
                                     BigDecimal subtotal, Instant createdAt) {
}
