package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import com.nexamarket.nexamarket.cart.infrastructure.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
public class CartCheckoutService {

    private final CartRepository cartRepository;
    private final OrderCreationGateway orderCreationGateway;
    private final Clock clock;

    @Autowired
    public CartCheckoutService(CartRepository cartRepository, OrderCreationGateway orderCreationGateway) {
        this(cartRepository, orderCreationGateway, Clock.systemUTC());
    }

    CartCheckoutService(CartRepository cartRepository, OrderCreationGateway orderCreationGateway, Clock clock) {
        this.cartRepository = cartRepository;
        this.orderCreationGateway = orderCreationGateway;
        this.clock = clock;
    }

    @Transactional
    public CheckoutCartView checkout(CheckoutCartCommand command) {
        Objects.requireNonNull(command, "Checkout command is required.");
        Objects.requireNonNull(command.customerId(), "Customer id is required.");

        Cart cart = cartRepository.findByCustomerIdAndStatusForUpdate(command.customerId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("No active cart was found for the customer."));
        if (cart.getItems().stream().anyMatch(item -> !item.getReservedUntil().isAfter(Instant.now(clock)))) {
            throw new IllegalStateException("The cart contains an expired stock reservation.");
        }

        OrderCreation orderCreation = orderCreationGateway.createOrder(CheckoutOrderRequest.from(cart));
        if (orderCreation == null || orderCreation.orderId() == null) {
            throw new IllegalStateException("Order service did not return an order id.");
        }
        cart.checkout();
        cartRepository.save(cart);
        return new CheckoutCartView(orderCreation.orderId());
    }
}
