package com.nexamarket.nexamarket.cart.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false, updatable = false)
    private Cart cart;

    @Column(name = "product_variant_id", nullable = false, updatable = false)
    private UUID productVariantId;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "reserved_until", nullable = false)
    private Instant reservedUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CartItem() {
    }

    CartItem(Cart cart, UUID productVariantId, UUID sellerId, int quantity, BigDecimal unitPrice,
             UUID reservationId, Instant reservedUntil) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least one.");
        }
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative.");
        }
        this.cart = Objects.requireNonNull(cart, "Cart is required.");
        this.productVariantId = Objects.requireNonNull(productVariantId, "Product variant id is required.");
        this.sellerId = Objects.requireNonNull(sellerId, "Seller id is required.");
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "Unit price is required.");
        this.reservationId = Objects.requireNonNull(reservationId, "Reservation id is required.");
        this.reservedUntil = Objects.requireNonNull(reservedUntil, "Reservation expiration is required.");
    }

    public boolean hasProductVariantAndSeller(UUID productVariantId, UUID sellerId) {
        return this.productVariantId.equals(productVariantId) && this.sellerId.equals(sellerId);
    }

    public void refreshReservation(int quantity, BigDecimal unitPrice, UUID reservationId, Instant reservedUntil) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least one.");
        }
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative.");
        }
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "Unit price is required.");
        this.reservationId = Objects.requireNonNull(reservationId, "Reservation id is required.");
        this.reservedUntil = Objects.requireNonNull(reservedUntil, "Reservation expiration is required.");
    }

    public UUID getId() {
        return id;
    }

    public Cart getCart() {
        return cart;
    }

    public UUID getProductVariantId() {
        return productVariantId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public Instant getReservedUntil() {
        return reservedUntil;
    }
}
