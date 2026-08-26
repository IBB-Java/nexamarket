package com.nexamarket.nexamarket.cart;

import com.nexamarket.nexamarket.cart.application.AddCartItemCommand;
import com.nexamarket.nexamarket.cart.application.CartApplicationService;
import com.nexamarket.nexamarket.cart.application.CartView;
import com.nexamarket.nexamarket.cart.application.StockReservation;
import com.nexamarket.nexamarket.cart.application.StockReservationGateway;
import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import com.nexamarket.nexamarket.cart.infrastructure.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartApplicationServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private StockReservationGateway stockReservationGateway;

    @Test
    void createsAReservationWhenAddingANewItem() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        StockReservation reservation = reservation(2);
        CartApplicationService service = new CartApplicationService(cartRepository, stockReservationGateway);

        when(cartRepository.findByCustomerIdAndStatusForUpdate(customerId, CartStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(stockReservationGateway.createReservation(customerId, variantId, sellerId, 2)).thenReturn(reservation);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartView cart = service.addItem(new AddCartItemCommand(customerId, variantId, sellerId, 2));

        assertThat(cart.status()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cart.items()).singleElement().satisfies(item -> {
            assertThat(item.productVariantId()).isEqualTo(variantId);
            assertThat(item.quantity()).isEqualTo(2);
            assertThat(item.unitPrice()).isEqualByComparingTo("499.90");
        });
        verify(stockReservationGateway).createReservation(customerId, variantId, sellerId, 2);
    }

    @Test
    void increasesTheExistingReservationInsteadOfCreatingAnotherItem() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        Cart existingCart = new Cart(customerId);
        existingCart.addItem(variantId, sellerId, 1, new BigDecimal("499.90"), reservationId,
                Instant.now().plusSeconds(600));
        CartApplicationService service = new CartApplicationService(cartRepository, stockReservationGateway);

        when(cartRepository.findByCustomerIdAndStatusForUpdate(customerId, CartStatus.ACTIVE))
                .thenReturn(Optional.of(existingCart));
        when(stockReservationGateway.increaseReservation(reservationId, 2)).thenReturn(reservation(3));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartView cart = service.addItem(new AddCartItemCommand(customerId, variantId, sellerId, 2));

        assertThat(cart.items()).singleElement().satisfies(item -> assertThat(item.quantity()).isEqualTo(3));
        verify(stockReservationGateway).increaseReservation(reservationId, 2);
        verify(stockReservationGateway, never()).createReservation(any(), any(), any(), any(Integer.class));
    }

    private StockReservation reservation(int quantity) {
        return new StockReservation(
                UUID.randomUUID(),
                quantity,
                new BigDecimal("499.90"),
                Instant.now().plusSeconds(600));
    }
}
