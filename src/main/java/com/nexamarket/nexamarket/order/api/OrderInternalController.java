package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.cart.application.OrderCreation;
import com.nexamarket.nexamarket.order.application.OrderCreationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/orders")
public class OrderInternalController {

    private final OrderCreationService orderCreationService;

    public OrderInternalController(OrderCreationService orderCreationService) {
        this.orderCreationService = orderCreationService;
    }

    @PostMapping("/from-cart")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderCreation createFromCart(@RequestBody CheckoutOrderRequest request) {
        return orderCreationService.createFromCart(request);
    }
}
