package com.nexamarket.stock.api;

import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.stock.application.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/stocks/reservations")
public class StockInternalController {

    private final StockService stockService;

    public StockInternalController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping
    public StockReservationResponse reserve(@Valid @RequestBody InternalReservationRequest request) {
        return stockService.reserve(new CreateStockReservationRequest(request.variantId(), request.quantity()),
                new AuthPrincipal(request.customerId(), "internal-customer-" + request.customerId(), UserRole.CUSTOMER));
    }

    @PatchMapping("/{reservationCode}")
    public StockReservationResponse increase(@PathVariable String reservationCode,
                                             @Valid @RequestBody IncreaseReservationRequest request) {
        return stockService.increaseReservationInternally(reservationCode, request.additionalQuantity());
    }

    @PostMapping("/{reservationCode}/confirm")
    public void confirm(@PathVariable String reservationCode) {
        stockService.confirmReservationInternally(reservationCode);
    }

    @DeleteMapping("/{reservationCode}")
    public void release(@PathVariable String reservationCode) {
        stockService.releaseReservationInternally(reservationCode);
    }

    public record InternalReservationRequest(@NotNull Long customerId, @NotNull Long variantId,
                                             @Min(1) int quantity) {
    }

    public record IncreaseReservationRequest(@Min(1) int additionalQuantity) {
    }
}
