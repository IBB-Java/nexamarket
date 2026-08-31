package com.nexamarket.users.api;

import com.nexamarket.auth.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Rol zorunludur") UserRole role
) {
}
