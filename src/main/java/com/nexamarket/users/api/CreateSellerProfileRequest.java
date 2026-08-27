package com.nexamarket.users.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSellerProfileRequest(
        @NotBlank @Size(max = 160) String storeName,
        @Size(max = 1000) String description
) {
}
