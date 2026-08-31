package com.nexamarket.nexamarket.order.api;

import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.nexamarket.order.application.CourierAssignmentService;
import com.nexamarket.nexamarket.order.application.OrderStatusService;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courier/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COURIER')")
public class CourierOrderController {

    private final CourierAssignmentService courierAssignmentService;
    private final OrderStatusService orderStatusService;

    @GetMapping
    public List<CourierOrderResponse> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return courierAssignmentService.listAssigned(principal.userId());
    }

    @PatchMapping("/{subOrderId}/status")
    public OrderStatus updateStatus(
            @PathVariable UUID subOrderId,
            @Valid @RequestBody UpdateSubOrderStatusRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return orderStatusService.updateSubOrderStatus(
                subOrderId, request.status(), principal.userId(), principal.role());
    }
}
