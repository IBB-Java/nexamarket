package com.nexamarket.users.api;

import com.nexamarket.users.application.UsersService;
import com.nexamarket.auth.security.AuthPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final UsersService usersService;

    @PatchMapping("/{userId}/status")
    public MyProfileResponse updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return usersService.updateUserStatus(userId, principal.userId(), request);
    }

    @PatchMapping("/{userId}/role")
    public MyProfileResponse updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return usersService.assignRole(userId, principal.userId(), request);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        usersService.deleteManagedUser(userId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
