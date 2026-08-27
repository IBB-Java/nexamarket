package com.nexamarket.auth.security;

import com.nexamarket.auth.entity.UserRole;

import java.time.Instant;

public record JwtPayload(String tokenId, Long userId, String email, UserRole role, String tokenType, Instant expiresAt) {
}
