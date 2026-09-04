package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.application.CustomerOrderHistoryService;
import com.nexamarket.nexamarket.order.application.OrderStatusService;
import com.nexamarket.nexamarket.order.application.RoleBasedOrderQueryService;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.auth.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderStatusService orderStatusService;
    private final CustomerOrderHistoryService customerOrderHistoryService;
    private final RoleBasedOrderQueryService roleBasedOrderQueryService;

    public OrderController(OrderStatusService orderStatusService,
                           CustomerOrderHistoryService customerOrderHistoryService,
                           RoleBasedOrderQueryService roleBasedOrderQueryService) {
        this.orderStatusService = orderStatusService;
        this.customerOrderHistoryService = customerOrderHistoryService;
        this.roleBasedOrderQueryService = roleBasedOrderQueryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SELLER', 'ADMIN')")
    public List<RoleOrderResponse> listVisibleOrders(@AuthenticationPrincipal AuthPrincipal principal) {
        return roleBasedOrderQueryService.listVisibleOrders(principal);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SELLER', 'ADMIN')")
    public List<RoleOrderResponse> getVisibleOrder(@PathVariable UUID orderId,
                                                   @AuthenticationPrincipal AuthPrincipal principal) {
        return roleBasedOrderQueryService.getVisibleOrder(orderId, principal);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<CustomerOrderSummaryResponse> listMyOrders(@AuthenticationPrincipal AuthPrincipal principal) {
        return customerOrderHistoryService.listForCustomer(principal.userId());
    }

    @PatchMapping("/{subOrderId}/status")
    public OrderStatus updateSubOrderStatus(@PathVariable UUID subOrderId,
                                            @Valid @RequestBody UpdateSubOrderStatusRequest request,
                                            @AuthenticationPrincipal AuthPrincipal principal) {
        return principal == null
                ? orderStatusService.updateSubOrderStatus(subOrderId, request.status())
                : orderStatusService.updateSubOrderStatus(subOrderId, request.status(), principal.userId(), principal.role());
    }
}
