package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartItem;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import com.nexamarket.nexamarket.cart.infrastructure.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class CartApplicationService {

    private final CartRepository cartRepository;
    private final StockReservationGateway stockReservationGateway;

    public CartApplicationService(CartRepository cartRepository, StockReservationGateway stockReservationGateway) {
        this.cartRepository = cartRepository;
        this.stockReservationGateway = stockReservationGateway;
    }

    /**
     * Serializes modifications to the same active cart. Stock consistency itself
     * is owned by the catalog service, which performs the reservation atomically.
     */
    @Transactional
    public CartView addItem(AddCartItemCommand command) {
        validate(command);
        Cart cart = cartRepository.findByCustomerIdAndStatusForUpdate(command.customerId(), CartStatus.ACTIVE)
                .orElseGet(() -> new Cart(command.customerId()));

        CartItem existingItem = cart.findItem(command.productVariantId(), command.sellerId());
        if (existingItem == null) {
            StockReservation reservation = stockReservationGateway.createReservation(
                    command.customerId(), command.productVariantId(), command.sellerId(), command.quantity());
            validateReservation(reservation, command.quantity());
            cart.addItem(
                    command.productVariantId(),
                    command.sellerId(),
                    reservation.reservedQuantity(),
                    reservation.unitPrice(),
                    reservation.reservationId(),
                    reservation.reservedUntil());
        } else {
            StockReservation reservation = stockReservationGateway.increaseReservation(
                    existingItem.getReservationId(), command.quantity());
            validateReservation(reservation, existingItem.getQuantity() + command.quantity());
            existingItem.refreshReservation(
                    reservation.reservedQuantity(),
                    reservation.unitPrice(),
                    reservation.reservationId(),
                    reservation.reservedUntil());
        }

        return CartView.from(cartRepository.save(cart));
    }

    private void validate(AddCartItemCommand command) {
        Objects.requireNonNull(command, "Add-cart-item command is required.");
        Objects.requireNonNull(command.customerId(), "Customer id is required.");
        Objects.requireNonNull(command.productVariantId(), "Product variant id is required.");
        Objects.requireNonNull(command.sellerId(), "Seller id is required.");
        if (command.quantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least one.");
        }
    }

    private void validateReservation(StockReservation reservation, int expectedQuantity) {
        Objects.requireNonNull(reservation, "Stock reservation is required.");
        Objects.requireNonNull(reservation.reservationId(), "Reservation id is required.");
        Objects.requireNonNull(reservation.unitPrice(), "Reservation unit price is required.");
        Objects.requireNonNull(reservation.reservedUntil(), "Reservation expiration is required.");
        if (reservation.reservedQuantity() != expectedQuantity) {
            throw new IllegalStateException("Catalog service returned an inconsistent reserved quantity.");
        }
    }
}
