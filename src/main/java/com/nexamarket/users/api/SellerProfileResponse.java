package com.nexamarket.users.api;

import com.nexamarket.users.entity.SellerProfile;
import com.nexamarket.users.entity.SellerProfileStatus;

import java.math.BigDecimal;

public record SellerProfileResponse(
        Long id,
        Long userId,
        String storeName,
        String description,
        BigDecimal commissionRate,
        SellerProfileStatus status
) {
    public static SellerProfileResponse from(SellerProfile profile) {
        return new SellerProfileResponse(
                profile.getId(), profile.getUser().getId(), profile.getStoreName(), profile.getDescription(),
                profile.getCommissionRate(), profile.getStatus());
    }
}
