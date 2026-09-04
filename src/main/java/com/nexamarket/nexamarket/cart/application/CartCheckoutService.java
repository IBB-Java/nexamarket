package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import com.nexamarket.nexamarket.cart.infrastructure.CartRepository;
import com.nexamarket.promotion.application.PromotionQuote;
import com.nexamarket.promotion.application.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.Objects;

@Service
public class CartCheckoutService {

    private final CartRepository cartRepository;
    private final OrderCreationGateway orderCreationGateway;
    private final Clock clock;
    private final PromotionService promotionService;

    @Autowired
    public CartCheckoutService(CartRepository cartRepository, OrderCreationGateway orderCreationGateway,
                               PromotionService promotionService) {
        this(cartRepository, orderCreationGateway, Clock.systemUTC(), promotionService);
    }

    CartCheckoutService(CartRepository cartRepository, OrderCreationGateway orderCreationGateway, Clock clock) {
        this(cartRepository, orderCreationGateway, clock, null);
    }

    CartCheckoutService(CartRepository cartRepository, OrderCreationGateway orderCreationGateway, Clock clock,
                        PromotionService promotionService) {
        this.cartRepository = cartRepository;
        this.orderCreationGateway = orderCreationGateway;
        this.clock = clock;
        this.promotionService = promotionService;
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

        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PromotionQuote promotionQuote = promotionService == null
                ? PromotionQuote.none()
                : promotionService.quote(command.promotionCodes(), subtotal);
        OrderCreation orderCreation = orderCreationGateway.createOrder(
                CheckoutOrderRequest.from(cart, promotionQuote, command.customerEmail()));
        if (orderCreation == null || orderCreation.orderId() == null) {
            throw new IllegalStateException("Order service did not return an order id.");
        }
        cart.checkout();
        cartRepository.save(cart);
        return new CheckoutCartView(orderCreation.orderId());
    }
}
