package com.nexamarket.loyalty.api;

import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.loyalty.application.LoyaltyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/me")
    public LoyaltyBalanceResponse myBalance(@AuthenticationPrincipal AuthPrincipal principal) {
        return new LoyaltyBalanceResponse(loyaltyService.balance(principal.userId()));
    }

    public record LoyaltyBalanceResponse(int points) {
    }
}
