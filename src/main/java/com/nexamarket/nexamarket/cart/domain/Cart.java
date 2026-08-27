package com.nexamarket.nexamarket.cart.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
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
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private Long customerId;

    @Column(name = "active_customer_id")
    private Long activeCustomerId;

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

    public Cart(Long customerId) {
        this.id = UUID.randomUUID();
        this.customerId = Objects.requireNonNull(customerId, "Customer id is required.");
        this.activeCustomerId = customerId;
        this.status = CartStatus.ACTIVE;
    }

    public CartItem addItem(Long productVariantId, Long sellerId, int quantity, BigDecimal unitPrice,
                            String reservationCode, Instant reservedUntil) {
        CartItem item = new CartItem(this,
                Objects.requireNonNull(productVariantId, "Product variant id is required."),
                Objects.requireNonNull(sellerId, "Seller id is required."), quantity,
                Objects.requireNonNull(unitPrice, "Unit price is required."),
                Objects.requireNonNull(reservationCode, "Reservation code is required."),
                Objects.requireNonNull(reservedUntil, "Reservation expiration is required."));
        items.add(item);
        return item;
    }

    public CartItem findItem(Long productVariantId, Long sellerId) {
        return items.stream().filter(item -> item.hasProductVariantAndSeller(productVariantId, sellerId)).findFirst().orElse(null);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
    }

    public void expireWhenEmpty() {
        if (items.isEmpty()) {
            status = CartStatus.EXPIRED;
            activeCustomerId = null;
        }
    }

    public void checkout() {
        if (status != CartStatus.ACTIVE) {
            throw new IllegalStateException("Only an active cart can be checked out.");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("An empty cart cannot be checked out.");
        }
        status = CartStatus.CHECKED_OUT;
        activeCustomerId = null;
    }

    public UUID getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public CartStatus getStatus() { return status; }
    public List<CartItem> getItems() { return Collections.unmodifiableList(items); }
}
