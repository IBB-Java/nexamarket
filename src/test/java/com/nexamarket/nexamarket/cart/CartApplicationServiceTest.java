package com.nexamarket.nexamarket.cart;

import com.nexamarket.nexamarket.cart.application.AddCartItemCommand;
import com.nexamarket.nexamarket.cart.application.CartApplicationService;
import com.nexamarket.nexamarket.cart.application.CatalogProductGateway;
import com.nexamarket.nexamarket.cart.application.CartView;
import com.nexamarket.nexamarket.cart.application.StockReservation;
import com.nexamarket.nexamarket.cart.application.StockReservationGateway;
import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import com.nexamarket.nexamarket.cart.infrastructure.CartRepository;
import com.nexamarket.common.integration.CatalogVariantSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

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

    @Mock
    private CatalogProductGateway catalogProductGateway;

    @Test
    void createsAReservationWhenAddingANewItem() {
        Long customerId = 11L;
        Long variantId = 12L;
        Long sellerId = 13L;
        StockReservation reservation = reservation(2);
        CartApplicationService service = new CartApplicationService(cartRepository, stockReservationGateway, catalogProductGateway);

        when(cartRepository.findByCustomerIdAndStatusForUpdate(customerId, CartStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(catalogProductGateway.findVariant(variantId)).thenReturn(variant(variantId, sellerId));
        when(stockReservationGateway.createReservation(customerId, variantId, 2)).thenReturn(reservation);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartView cart = service.addItem(new AddCartItemCommand(customerId, variantId, 2));

        assertThat(cart.status()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cart.items()).singleElement().satisfies(item -> {
            assertThat(item.productVariantId()).isEqualTo(variantId);
            assertThat(item.quantity()).isEqualTo(2);
            assertThat(item.unitPrice()).isEqualByComparingTo("499.90");
        });
        verify(stockReservationGateway).createReservation(customerId, variantId, 2);
    }

    @Test
    void increasesTheExistingReservationInsteadOfCreatingAnotherItem() {
        Long customerId = 21L;
        Long variantId = 22L;
        Long sellerId = 23L;
        String reservationCode = "reservation-22";
        Cart existingCart = new Cart(customerId);
        existingCart.addItem(variantId, sellerId, 1, new BigDecimal("499.90"), reservationCode,
                Instant.now().plusSeconds(600));
        CartApplicationService service = new CartApplicationService(cartRepository, stockReservationGateway, catalogProductGateway);

        when(cartRepository.findByCustomerIdAndStatusForUpdate(customerId, CartStatus.ACTIVE))
                .thenReturn(Optional.of(existingCart));
        when(catalogProductGateway.findVariant(variantId)).thenReturn(variant(variantId, sellerId));
        when(stockReservationGateway.increaseReservation(reservationCode, 2)).thenReturn(reservation(3));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartView cart = service.addItem(new AddCartItemCommand(customerId, variantId, 2));

        assertThat(cart.items()).singleElement().satisfies(item -> assertThat(item.quantity()).isEqualTo(3));
        verify(stockReservationGateway).increaseReservation(reservationCode, 2);
        verify(stockReservationGateway, never()).createReservation(any(), any(), any(Integer.class));
    }

    @Test
    void releasesReservationWhenRemovingAnItem() {
        Long customerId = 31L;
        Cart cart = new Cart(customerId);
        var item = cart.addItem(32L, 33L, 1, new BigDecimal("49.90"), "remove-reservation",
                Instant.now().plusSeconds(600));
        ReflectionTestUtils.setField(item, "id", java.util.UUID.randomUUID());
        CartApplicationService service = new CartApplicationService(cartRepository, stockReservationGateway, catalogProductGateway);

        when(cartRepository.findByCustomerIdAndStatusForUpdate(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartView updated = service.removeItem(customerId, item.getId());

        assertThat(updated.items()).isEmpty();
        verify(stockReservationGateway).releaseReservation("remove-reservation");
    }

    @Test
    void removesOnlyOneUnitWhenTheCartLineContainsSeveralUnits() {
        Long customerId = 41L;
        Cart cart = new Cart(customerId);
        var item = cart.addItem(42L, 43L, 3, new BigDecimal("49.90"), "decrease-reservation",
                Instant.now().plusSeconds(600));
        ReflectionTestUtils.setField(item, "id", java.util.UUID.randomUUID());
        CartApplicationService service = new CartApplicationService(cartRepository, stockReservationGateway, catalogProductGateway);

        when(cartRepository.findByCustomerIdAndStatusForUpdate(customerId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(stockReservationGateway.decreaseReservation("decrease-reservation", 1)).thenReturn(reservation(2));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartView updated = service.removeItem(customerId, item.getId());

        assertThat(updated.items()).singleElement().satisfies(view -> assertThat(view.quantity()).isEqualTo(2));
        verify(stockReservationGateway).decreaseReservation("decrease-reservation", 1);
        verify(stockReservationGateway, never()).releaseReservation("decrease-reservation");
    }

    private StockReservation reservation(int quantity) {
        return new StockReservation(
                "reservation-" + quantity,
                quantity,
                new BigDecimal("499.90"),
                Instant.now().plusSeconds(600));
    }

    private CatalogVariantSnapshot variant(Long variantId, Long sellerId) {
        return new CatalogVariantSnapshot(variantId, 1L, sellerId, "Test ürünü", new BigDecimal("499.90"));
    }
}
