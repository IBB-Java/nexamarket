package com.nexamarket.nexamarket.order.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectDeliveryRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
