package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.DeliveryFailureReasonCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FailDeliveryRequest(
        @NotNull DeliveryFailureReasonCode reasonCode,
        @NotBlank @Size(max = 1000) String description
) {
}
