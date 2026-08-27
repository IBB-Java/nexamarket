package com.nexamarket.users.api;

import com.nexamarket.users.entity.SellerProfileStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewSellerProfileRequest(@NotNull SellerProfileStatus status) {
}
