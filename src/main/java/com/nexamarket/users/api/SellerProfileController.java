package com.nexamarket.users.api;

import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.users.application.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerProfileController {

    private final UsersService usersService;

    @PostMapping("/me")
    public ResponseEntity<SellerProfileResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateSellerProfileRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usersService.createSellerProfile(principal.userId(), request));
    }

    @GetMapping("/me")
    public SellerProfileResponse getMine(@AuthenticationPrincipal AuthPrincipal principal) {
        return usersService.getMySellerProfile(principal.userId());
    }

    @PatchMapping("/me")
    public SellerProfileResponse updateMine(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UpdateSellerProfileRequest request
    ) {
        return usersService.updateMySellerProfile(principal.userId(), request);
    }

    @GetMapping("/{sellerId}")
    public SellerProfileResponse getPublic(@PathVariable Long sellerId) {
        return usersService.getActiveSellerProfile(sellerId);
    }
}
