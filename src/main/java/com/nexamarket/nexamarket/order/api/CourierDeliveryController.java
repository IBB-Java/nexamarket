package com.nexamarket.nexamarket.order.api;

import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.nexamarket.order.application.DeliveryAssignmentService;
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
@RequestMapping("/api/v1/courier/deliveries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COURIER')")
public class CourierDeliveryController {

    private final DeliveryAssignmentService deliveryAssignmentService;

    @GetMapping
    public List<CourierDeliveryResponse> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return deliveryAssignmentService.listForCourier(principal.userId());
    }

    @PatchMapping("/{assignmentId}/accept")
    public CourierDeliveryResponse accept(@PathVariable UUID assignmentId,
                                          @AuthenticationPrincipal AuthPrincipal principal) {
        return deliveryAssignmentService.accept(assignmentId, principal.userId());
    }

    @PatchMapping("/{assignmentId}/reject")
    public CourierDeliveryResponse reject(@PathVariable UUID assignmentId,
                                          @Valid @RequestBody RejectDeliveryRequest request,
                                          @AuthenticationPrincipal AuthPrincipal principal) {
        return deliveryAssignmentService.reject(assignmentId, principal.userId(), request.reason());
    }

    @PatchMapping("/{assignmentId}/pickup")
    public CourierDeliveryResponse pickup(@PathVariable UUID assignmentId,
                                          @AuthenticationPrincipal AuthPrincipal principal) {
        return deliveryAssignmentService.pickup(assignmentId, principal.userId());
    }

    @PatchMapping("/{assignmentId}/start")
    public CourierDeliveryResponse start(@PathVariable UUID assignmentId,
                                         @AuthenticationPrincipal AuthPrincipal principal) {
        return deliveryAssignmentService.start(assignmentId, principal.userId());
    }

    @PatchMapping("/{assignmentId}/deliver")
    public CourierDeliveryResponse deliver(@PathVariable UUID assignmentId,
                                           @AuthenticationPrincipal AuthPrincipal principal) {
        return deliveryAssignmentService.deliver(assignmentId, principal.userId());
    }

    @PatchMapping("/{assignmentId}/fail")
    public CourierDeliveryResponse fail(@PathVariable UUID assignmentId,
                                        @Valid @RequestBody FailDeliveryRequest request,
                                        @AuthenticationPrincipal AuthPrincipal principal) {
        return deliveryAssignmentService.fail(
                assignmentId, principal.userId(), request.reasonCode(), request.description());
    }
}
