package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartItem;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import com.nexamarket.nexamarket.cart.infrastructure.CartRepository;
import com.nexamarket.catalog.entity.ProductVariant;
import com.nexamarket.catalog.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class CartApplicationService {

    private final CartRepository cartRepository;
    private final StockReservationGateway stockReservationGateway;
    private final ProductVariantRepository productVariantRepository;

    public CartApplicationService(CartRepository cartRepository, StockReservationGateway stockReservationGateway,
                                  ProductVariantRepository productVariantRepository) {
        this.cartRepository = cartRepository;
        this.stockReservationGateway = stockReservationGateway;
        this.productVariantRepository = productVariantRepository;
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

        ProductVariant variant = productVariantRepository.findById(command.productVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Product variant was not found."));
        Long sellerId = variant.getProduct().getSellerId();

        CartItem existingItem = cart.findItem(command.productVariantId(), sellerId);
        if (existingItem == null) {
            StockReservation reservation = stockReservationGateway.createReservation(
                    command.customerId(), command.productVariantId(), command.quantity());
            validateReservation(reservation, command.quantity());
            cart.addItem(
                    command.productVariantId(),
                    sellerId,
                    reservation.reservedQuantity(),
                    reservation.unitPrice(),
                    reservation.reservationCode(),
                    reservation.reservedUntil());
        } else {
            StockReservation reservation = stockReservationGateway.increaseReservation(
                    existingItem.getReservationCode(), command.quantity());
            validateReservation(reservation, existingItem.getQuantity() + command.quantity());
            existingItem.refreshReservation(
                    reservation.reservedQuantity(),
                    reservation.unitPrice(),
                    reservation.reservationCode(),
                    reservation.reservedUntil());
        }

        return CartView.from(cartRepository.save(cart));
    }

    private void validate(AddCartItemCommand command) {
        Objects.requireNonNull(command, "Add-cart-item command is required.");
        Objects.requireNonNull(command.customerId(), "Customer id is required.");
        Objects.requireNonNull(command.productVariantId(), "Product variant id is required.");
        if (command.quantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least one.");
        }
    }

    private void validateReservation(StockReservation reservation, int expectedQuantity) {
        Objects.requireNonNull(reservation, "Stock reservation is required.");
        Objects.requireNonNull(reservation.reservationCode(), "Reservation code is required.");
        Objects.requireNonNull(reservation.unitPrice(), "Reservation unit price is required.");
        Objects.requireNonNull(reservation.reservedUntil(), "Reservation expiration is required.");
        if (reservation.reservedQuantity() != expectedQuantity) {
            throw new IllegalStateException("Catalog service returned an inconsistent reserved quantity.");
        }
    }
}
