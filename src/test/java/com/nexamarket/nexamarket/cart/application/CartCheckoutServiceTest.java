package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import com.nexamarket.nexamarket.cart.infrastructure.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartCheckoutServiceTest {

    private final Instant now = Instant.parse("2026-08-26T12:00:00Z");

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderCreationGateway orderCreationGateway;

    @Test
    void createsOneOrderRequestGroupedBySellerAndChecksOutTheCart() {
        Long customerId = 201L;
        Long firstSellerId = 202L;
        Long secondSellerId = 203L;
        Cart cart = new Cart(customerId);
        cart.addItem(204L, firstSellerId, 1, new BigDecimal("25.00"), "reservation-204", now.plusSeconds(600));
        cart.addItem(205L, firstSellerId, 2, new BigDecimal("50.00"), "reservation-205", now.plusSeconds(600));
        cart.addItem(206L, secondSellerId, 1, new BigDecimal("75.00"), "reservation-206", now.plusSeconds(600));
        UUID orderId = UUID.randomUUID();
        CartCheckoutService service = service();
        when(cartRepository.findByCustomerIdAndStatusForUpdate(customerId, CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(orderCreationGateway.createOrder(any())).thenReturn(new OrderCreation(orderId));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutCartView response = service.checkout(new CheckoutCartCommand(customerId));

        ArgumentCaptor<CheckoutOrderRequest> requestCaptor = ArgumentCaptor.forClass(CheckoutOrderRequest.class);
        verify(orderCreationGateway).createOrder(requestCaptor.capture());
        assertThat(requestCaptor.getValue().customerId()).isEqualTo(customerId);
        assertThat(requestCaptor.getValue().sellerOrders())
                .hasSize(2)
                .anySatisfy(sellerOrder -> {
                    assertThat(sellerOrder.sellerId()).isEqualTo(firstSellerId);
                    assertThat(sellerOrder.items()).hasSize(2);
                })
                .anySatisfy(sellerOrder -> {
                    assertThat(sellerOrder.sellerId()).isEqualTo(secondSellerId);
                    assertThat(sellerOrder.items()).hasSize(1);
                });
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
    }

    @Test
    void rejectsCheckoutWhenAStockReservationHasExpired() {
        Long customerId = 211L;
        Cart cart = new Cart(customerId);
        cart.addItem(212L, 213L, 1, new BigDecimal("25.00"), "reservation-212", now.minusSeconds(1));
        CartCheckoutService service = service();
        when(cartRepository.findByCustomerIdAndStatusForUpdate(customerId, CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> service.checkout(new CheckoutCartCommand(customerId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The cart contains an expired stock reservation.");

        verify(orderCreationGateway, never()).createOrder(any());
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
    }

    private CartCheckoutService service() {
        return new CartCheckoutService(
                cartRepository,
                orderCreationGateway,
                Clock.fixed(now, ZoneOffset.UTC));
    }
}
