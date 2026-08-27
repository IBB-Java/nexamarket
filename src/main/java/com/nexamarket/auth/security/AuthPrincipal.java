package com.nexamarket.auth.security;

import com.nexamarket.auth.entity.UserRole;

public record AuthPrincipal(Long userId, String email, UserRole role) {
}
