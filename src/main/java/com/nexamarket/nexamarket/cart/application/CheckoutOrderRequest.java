package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record CheckoutOrderRequest(UUID customerId, List<SellerOrderRequest> sellerOrders) {

    public static CheckoutOrderRequest from(Cart cart) {
        Map<UUID, List<OrderItemRequest>> itemsBySeller = cart.getItems().stream()
                .collect(Collectors.groupingBy(
                        CartItem::getSellerId,
                        Collectors.mapping(OrderItemRequest::from, Collectors.toList())));

        List<SellerOrderRequest> sellerOrders = itemsBySeller.entrySet().stream()
                .map(entry -> new SellerOrderRequest(entry.getKey(), entry.getValue()))
                .toList();
        return new CheckoutOrderRequest(cart.getCustomerId(), sellerOrders);
    }

    public record SellerOrderRequest(UUID sellerId, List<OrderItemRequest> items) {
    }

    public record OrderItemRequest(UUID productVariantId, int quantity, BigDecimal unitPrice,
                                   UUID stockReservationId, Instant reservedUntil) {

        private static OrderItemRequest from(CartItem item) {
            return new OrderItemRequest(
                    item.getProductVariantId(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getReservationId(),
                    item.getReservedUntil());
        }
    }
}
