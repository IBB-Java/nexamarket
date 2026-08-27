package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record CheckoutOrderRequest(UUID sourceCartId, Long customerId, List<SellerOrderRequest> sellerOrders) {

    public static CheckoutOrderRequest from(Cart cart) {
        Map<Long, List<OrderItemRequest>> itemsBySeller = cart.getItems().stream()
                .collect(Collectors.groupingBy(
                        CartItem::getSellerId,
                        Collectors.mapping(OrderItemRequest::from, Collectors.toList())));

        List<SellerOrderRequest> sellerOrders = itemsBySeller.entrySet().stream()
                .map(entry -> new SellerOrderRequest(entry.getKey(), entry.getValue()))
                .toList();
        return new CheckoutOrderRequest(cart.getId(), cart.getCustomerId(), sellerOrders);
    }

    public record SellerOrderRequest(Long sellerId, List<OrderItemRequest> items) {
    }

    public record OrderItemRequest(Long productVariantId, int quantity, BigDecimal unitPrice,
                                   String stockReservationCode, Instant reservedUntil) {

        private static OrderItemRequest from(CartItem item) {
            return new OrderItemRequest(
                    item.getProductVariantId(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getReservationCode(),
                    item.getReservedUntil());
        }
    }
}
