package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.application.DeliveryAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final DeliveryAssignmentService deliveryAssignmentService;

    @PatchMapping("/{subOrderId}/courier")
    public AdminDeliveryAssignmentResponse assignCourier(
            @PathVariable UUID subOrderId,
            @Valid @RequestBody AssignCourierRequest request
    ) {
        return deliveryAssignmentService.assign(subOrderId, request.courierId());
    }
}
