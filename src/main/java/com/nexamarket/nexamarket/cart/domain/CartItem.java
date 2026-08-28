package com.nexamarket.nexamarket.cart.domain;

import com.nexamarket.catalog.entity.ProductVariant;
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
    private Long productVariantId;

    /** Read-only link used to build a customer-friendly cart response. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", insertable = false, updatable = false)
    private ProductVariant productVariant;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private Long sellerId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "reservation_code", nullable = false, length = 36)
    private String reservationCode;

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

    CartItem(Cart cart, Long productVariantId, Long sellerId, int quantity, BigDecimal unitPrice,
             String reservationCode, Instant reservedUntil) {
        validate(quantity, unitPrice);
        this.cart = Objects.requireNonNull(cart, "Cart is required.");
        this.productVariantId = Objects.requireNonNull(productVariantId, "Product variant id is required.");
        this.sellerId = Objects.requireNonNull(sellerId, "Seller id is required.");
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.reservationCode = Objects.requireNonNull(reservationCode, "Reservation code is required.");
        this.reservedUntil = Objects.requireNonNull(reservedUntil, "Reservation expiration is required.");
    }

    public boolean hasProductVariantAndSeller(Long productVariantId, Long sellerId) {
        return this.productVariantId.equals(productVariantId) && this.sellerId.equals(sellerId);
    }

    public void refreshReservation(int quantity, BigDecimal unitPrice, String reservationCode, Instant reservedUntil) {
        validate(quantity, unitPrice);
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.reservationCode = Objects.requireNonNull(reservationCode, "Reservation code is required.");
        this.reservedUntil = Objects.requireNonNull(reservedUntil, "Reservation expiration is required.");
    }

    private static void validate(int quantity, BigDecimal unitPrice) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be at least one.");
        if (unitPrice == null || unitPrice.signum() < 0) throw new IllegalArgumentException("Unit price cannot be negative.");
    }

    public UUID getId() { return id; }
    public Cart getCart() { return cart; }
    public Long getProductVariantId() { return productVariantId; }
    public ProductVariant getProductVariant() { return productVariant; }
    public Long getSellerId() { return sellerId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getReservationCode() { return reservationCode; }
    public Instant getReservedUntil() { return reservedUntil; }
}
