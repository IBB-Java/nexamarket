package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.cart.application.OrderCreation;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class OrderCreationService {

    private final CustomerOrderRepository customerOrderRepository;

    public OrderCreationService(CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    /**
     * sourceCartId is the idempotency key: a retried checkout returns the
     * original order instead of creating another set of sub-orders.
     */
    @Transactional
    public OrderCreation createFromCart(CheckoutOrderRequest request) {
        Objects.requireNonNull(request, "Checkout request is required.");
        Objects.requireNonNull(request.sourceCartId(), "Source cart id is required.");

        return customerOrderRepository.findBySourceCartId(request.sourceCartId())
                .map(existing -> new OrderCreation(existing.getId()))
                .orElseGet(() -> {
                    CustomerOrder order = CustomerOrder.from(request);
                    CustomerOrder savedOrder = customerOrderRepository.save(order);
                    return new OrderCreation(savedOrder.getId());
                });
    }
}
