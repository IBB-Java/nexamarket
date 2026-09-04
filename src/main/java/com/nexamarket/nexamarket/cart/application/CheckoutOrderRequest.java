package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.nexamarket.promotion.application.PromotionQuote;

public record CheckoutOrderRequest(UUID sourceCartId, Long customerId, List<SellerOrderRequest> sellerOrders,
                                   BigDecimal discountAmount, List<String> promotionCodes, String customerEmail) {

    public CheckoutOrderRequest(UUID sourceCartId, Long customerId, List<SellerOrderRequest> sellerOrders) {
        this(sourceCartId, customerId, sellerOrders, BigDecimal.ZERO, List.of(), null);
    }

    /** Backward-compatible constructor kept for the original module tests. */
    public CheckoutOrderRequest(Long customerId, UUID sourceCartId, List<SellerOrderRequest> sellerOrders) {
        this(sourceCartId, customerId, sellerOrders, BigDecimal.ZERO, List.of(), null);
    }

    public CheckoutOrderRequest {
        discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        promotionCodes = promotionCodes == null ? List.of() : List.copyOf(promotionCodes);
    }

    public static CheckoutOrderRequest from(Cart cart) {
        return from(cart, PromotionQuote.none());
    }

    public static CheckoutOrderRequest from(Cart cart, PromotionQuote promotionQuote) {
        return from(cart, promotionQuote, null);
    }

    public static CheckoutOrderRequest from(Cart cart, PromotionQuote promotionQuote, String customerEmail) {
        Map<Long, List<OrderItemRequest>> itemsBySeller = cart.getItems().stream()
                .collect(Collectors.groupingBy(
                        CartItem::getSellerId,
                        Collectors.mapping(OrderItemRequest::from, Collectors.toList())));

        List<SellerOrderRequest> sellerOrders = itemsBySeller.entrySet().stream()
                .map(entry -> new SellerOrderRequest(entry.getKey(), entry.getValue()))
                .toList();
        return new CheckoutOrderRequest(cart.getId(), cart.getCustomerId(), sellerOrders,
                promotionQuote.discountAmount(), promotionQuote.appliedCodes(), customerEmail);
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
