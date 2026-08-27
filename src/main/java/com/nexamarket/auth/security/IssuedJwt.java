package com.nexamarket.auth.security;

import java.time.Instant;

public record IssuedJwt(String value, String tokenId, Instant expiresAt) {
}
