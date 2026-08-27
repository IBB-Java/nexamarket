package com.nexamarket.users.api;

import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.users.entity.UserProfile;

public record MyProfileResponse(
        Long userId,
        String email,
        UserRole role,
        UserStatus status,
        String firstName,
        String lastName,
        String phoneNumber
) {
    public static MyProfileResponse from(UserAccount user, UserProfile profile) {
        return new MyProfileResponse(
                user.getId(), user.getEmail(), user.getRole(), user.getStatus(),
                profile == null ? null : profile.getFirstName(),
                profile == null ? null : profile.getLastName(),
                profile == null ? null : profile.getPhoneNumber());
    }
}
