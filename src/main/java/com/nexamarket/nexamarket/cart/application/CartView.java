package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartItem;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import com.nexamarket.catalog.entity.ProductVariant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartView(UUID id, CartStatus status, List<CartItemView> items) {

    public static CartView empty() {
        return new CartView(null, null, List.of());
    }

    public static CartView from(Cart cart) {
        return new CartView(
                cart.getId(),
                cart.getStatus(),
                cart.getItems().stream().map(CartItemView::from).toList());
    }

    public record CartItemView(UUID id, Long productVariantId, Long sellerId, int quantity,
                               BigDecimal unitPrice, Instant reservedUntil,
                               Long productId, String productName) {

        private static CartItemView from(CartItem item) {
            ProductVariant variant = item.getProductVariant();
            return new CartItemView(
                    item.getId(),
                    item.getProductVariantId(),
                    item.getSellerId(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getReservedUntil(),
                    variant == null || variant.getProduct() == null ? null : variant.getProduct().getId(),
                    variant == null || variant.getProduct() == null ? null : variant.getProduct().getName());
        }
    }
}
