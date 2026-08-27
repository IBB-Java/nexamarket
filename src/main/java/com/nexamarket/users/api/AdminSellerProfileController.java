package com.nexamarket.users.api;

import com.nexamarket.users.application.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sellers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSellerProfileController {

    private final UsersService usersService;

    @GetMapping("/pending")
    public List<SellerProfileResponse> pending() {
        return usersService.listPendingSellerProfiles();
    }

    @PatchMapping("/{sellerId}/status")
    public SellerProfileResponse review(
            @PathVariable Long sellerId,
            @Valid @RequestBody ReviewSellerProfileRequest request
    ) {
        return usersService.reviewSellerProfile(sellerId, request);
    }
}
