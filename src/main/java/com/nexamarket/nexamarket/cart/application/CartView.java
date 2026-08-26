package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartItem;
import com.nexamarket.nexamarket.cart.domain.CartStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartView(UUID id, CartStatus status, List<CartItemView> items) {

    public static CartView from(Cart cart) {
        return new CartView(
                cart.getId(),
                cart.getStatus(),
                cart.getItems().stream().map(CartItemView::from).toList());
    }

    public record CartItemView(UUID id, UUID productVariantId, UUID sellerId, int quantity,
                               BigDecimal unitPrice, Instant reservedUntil) {

        private static CartItemView from(CartItem item) {
            return new CartItemView(
                    item.getId(),
                    item.getProductVariantId(),
                    item.getSellerId(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getReservedUntil());
        }
    }
}
