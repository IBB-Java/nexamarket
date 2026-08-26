package com.nexamarket.nexamarket.cart.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "active_customer_id")
    private UUID activeCustomerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CartStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public Cart(UUID customerId) {
        this.customerId = Objects.requireNonNull(customerId, "Customer id is required.");
        this.activeCustomerId = customerId;
        this.status = CartStatus.ACTIVE;
    }

    public CartItem addItem(UUID productVariantId, UUID sellerId, int quantity, java.math.BigDecimal unitPrice,
                            UUID reservationId, Instant reservedUntil) {
        CartItem item = new CartItem(
                this,
                Objects.requireNonNull(productVariantId, "Product variant id is required."),
                Objects.requireNonNull(sellerId, "Seller id is required."),
                quantity,
                Objects.requireNonNull(unitPrice, "Unit price is required."),
                Objects.requireNonNull(reservationId, "Reservation id is required."),
                Objects.requireNonNull(reservedUntil, "Reservation expiration is required."));
        items.add(item);
        return item;
    }

    public CartItem findItem(UUID productVariantId, UUID sellerId) {
        return items.stream()
                .filter(item -> item.hasProductVariantAndSeller(productVariantId, sellerId))
                .findFirst()
                .orElse(null);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
    }

    public void expireWhenEmpty() {
        if (!items.isEmpty()) {
            return;
        }
        status = CartStatus.EXPIRED;
        activeCustomerId = null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public CartStatus getStatus() {
        return status;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
