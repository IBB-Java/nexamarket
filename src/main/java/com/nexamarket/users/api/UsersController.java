package com.nexamarket.users.api;

import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.users.application.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @GetMapping
    public MyProfileResponse get(@AuthenticationPrincipal AuthPrincipal principal) {
        return usersService.getMyProfile(principal.userId());
    }

    @PatchMapping
    public MyProfileResponse update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        return usersService.updateMyProfile(principal.userId(), request);
    }
}
