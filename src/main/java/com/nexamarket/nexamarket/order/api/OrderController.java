package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.application.OrderStatusService;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderStatusService orderStatusService;

    public OrderController(OrderStatusService orderStatusService) {
        this.orderStatusService = orderStatusService;
    }

    @PatchMapping("/{subOrderId}/status")
    public OrderStatus updateSubOrderStatus(@PathVariable UUID subOrderId,
                                            @Valid @RequestBody UpdateSubOrderStatusRequest request) {
        return orderStatusService.updateSubOrderStatus(subOrderId, request.status());
    }
}
