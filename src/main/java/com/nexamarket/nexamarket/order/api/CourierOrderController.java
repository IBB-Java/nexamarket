package com.nexamarket.nexamarket.order.api;

import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.nexamarket.order.application.DeliveryAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courier/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COURIER')")
public class CourierOrderController {

    private final DeliveryAssignmentService deliveryAssignmentService;

    @GetMapping
    public List<CourierDeliveryResponse> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return deliveryAssignmentService.listForCourier(principal.userId());
    }
}
