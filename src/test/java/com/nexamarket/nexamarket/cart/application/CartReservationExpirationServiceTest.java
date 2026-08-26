package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import com.nexamarket.nexamarket.cart.infrastructure.CartItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartReservationExpirationServiceTest {

    private final Instant now = Instant.parse("2026-08-26T12:00:00Z");

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private StockReservationGateway stockReservationGateway;

    @Test
    void releasesAnExpiredReservationAndExpiresAnEmptyCart() {
        Cart cart = new Cart(UUID.randomUUID());
        UUID reservationId = UUID.randomUUID();
        var item = cart.addItem(UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("49.90"), reservationId,
                now.minusSeconds(1));
        CartReservationExpirationService service = service();
        when(cartItemRepository.findExpiredItemsForUpdate(now, CartStatus.ACTIVE)).thenReturn(List.of(item));

        int releasedItemCount = service.releaseExpiredReservations();

        assertThat(releasedItemCount).isEqualTo(1);
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getStatus()).isEqualTo(CartStatus.EXPIRED);
        verify(stockReservationGateway).releaseReservation(reservationId);
    }

    @Test
    void keepsTheCartItemWhenCatalogCannotConfirmRelease() {
        Cart cart = new Cart(UUID.randomUUID());
        UUID reservationId = UUID.randomUUID();
        var item = cart.addItem(UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("49.90"), reservationId,
                now.minusSeconds(1));
        CartReservationExpirationService service = service();
        when(cartItemRepository.findExpiredItemsForUpdate(now, CartStatus.ACTIVE)).thenReturn(List.of(item));
        doThrow(new IllegalStateException("Catalog service is unavailable"))
                .when(stockReservationGateway).releaseReservation(reservationId);

        assertThatThrownBy(service::releaseExpiredReservations)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Catalog service is unavailable");

        assertThat(cart.getItems()).containsExactly(item);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
    }

    private CartReservationExpirationService service() {
        return new CartReservationExpirationService(
                cartItemRepository,
                stockReservationGateway,
                Clock.fixed(now, ZoneOffset.UTC));
    }
}
