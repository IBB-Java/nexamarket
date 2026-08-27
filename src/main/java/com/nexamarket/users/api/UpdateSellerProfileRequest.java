package com.nexamarket.users.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record UpdateSellerProfileRequest(
        @Size(max = 160) String storeName,
        @Size(max = 1000) String description
) {
    @AssertTrue(message = "En az bir mağaza alanı güncellenmelidir")
    public boolean isChangeRequested() {
        return storeName != null || description != null;
    }
}
