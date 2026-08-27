package com.nexamarket.stock.api;

import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.stock.application.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/variants/{variantId}")
    public StockLevelResponse getStockLevel(@PathVariable Long variantId) {
        return stockService.getStockLevel(variantId);
    }

    @PatchMapping("/variants/{variantId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public StockLevelResponse updateStockLevel(
            @PathVariable Long variantId,
            @Valid @RequestBody UpdateStockQuantityRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return stockService.updateStockLevel(variantId, request, principal);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public StockReservationResponse reserve(
            @Valid @RequestBody CreateStockReservationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return stockService.reserve(request, principal);
    }

    @PostMapping("/reservations/{reservationCode}/confirm")
    public StockReservationResponse confirm(
            @PathVariable String reservationCode,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return stockService.confirm(reservationCode, principal);
    }

    @DeleteMapping("/reservations/{reservationCode}")
    public StockReservationResponse release(
            @PathVariable String reservationCode,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return stockService.release(reservationCode, principal);
    }
}
