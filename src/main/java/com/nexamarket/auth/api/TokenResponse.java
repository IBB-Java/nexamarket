package com.nexamarket.auth.api;

import com.nexamarket.auth.entity.UserRole;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        long refreshTokenExpiresInSeconds,
        UserRole role
) {
}
