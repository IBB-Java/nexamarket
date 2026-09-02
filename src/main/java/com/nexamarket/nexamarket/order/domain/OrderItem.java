package com.nexamarket.nexamarket.order.domain;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_order_id", nullable = false, updatable = false)
    private SubOrder subOrder;

    @Column(name = "product_variant_id", nullable = false, updatable = false)
    private Long productVariantId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "stock_reservation_code", nullable = false, updatable = false, length = 36)
    private String stockReservationCode;

    @Column(name = "reserved_until", nullable = false, updatable = false)
    private Instant reservedUntil;

    protected OrderItem() {
    }

    private OrderItem(SubOrder subOrder, Long productVariantId, int quantity, BigDecimal unitPrice,
                      String stockReservationCode, Instant reservedUntil) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least one.");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative.");
        }
        this.id = UUID.randomUUID();
        this.subOrder = Objects.requireNonNull(subOrder, "Sub-order is required.");
        this.productVariantId = Objects.requireNonNull(productVariantId, "Product variant id is required.");
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.stockReservationCode = Objects.requireNonNull(stockReservationCode, "Stock reservation code is required.");
        this.reservedUntil = Objects.requireNonNull(reservedUntil, "Reservation expiration is required.");
    }

    static OrderItem from(SubOrder subOrder, CheckoutOrderRequest.OrderItemRequest request) {
        return new OrderItem(
                subOrder,
                request.productVariantId(),
                request.quantity(),
                request.unitPrice(),
                request.stockReservationCode(),
                request.reservedUntil());
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public int getQuantity() {
        return quantity;
    }

    public String getStockReservationCode() {
        return stockReservationCode;
    }
}
