package com.nexamarket.auth.api;

import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;

public record UserResponse(Long id, String email, UserRole role, UserStatus status) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus());
    }
}
