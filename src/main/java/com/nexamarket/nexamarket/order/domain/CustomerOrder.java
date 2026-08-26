package com.nexamarket.nexamarket.order.domain;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "orders")
public class CustomerOrder {

    @Id
    private UUID id;

    @Column(name = "source_cart_id", nullable = false, unique = true, updatable = false)
    private UUID sourceCartId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<SubOrder> subOrders = new ArrayList<>();

    protected CustomerOrder() {
    }

    private CustomerOrder(UUID sourceCartId, UUID customerId) {
        this.id = UUID.randomUUID();
        this.sourceCartId = Objects.requireNonNull(sourceCartId, "Source cart id is required.");
        this.customerId = Objects.requireNonNull(customerId, "Customer id is required.");
        this.status = OrderStatus.PAYMENT_PENDING;
        this.totalAmount = BigDecimal.ZERO;
    }

    public static CustomerOrder from(CheckoutOrderRequest request) {
        Objects.requireNonNull(request, "Checkout request is required.");
        if (request.sellerOrders() == null || request.sellerOrders().isEmpty()) {
            throw new IllegalArgumentException("At least one seller order is required.");
        }

        CustomerOrder order = new CustomerOrder(request.sourceCartId(), request.customerId());
        for (CheckoutOrderRequest.SellerOrderRequest sellerOrderRequest : request.sellerOrders()) {
            SubOrder subOrder = SubOrder.from(order, sellerOrderRequest);
            order.subOrders.add(subOrder);
            order.totalAmount = order.totalAmount.add(subOrder.getSubtotal());
        }
        return order;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceCartId() {
        return sourceCartId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    void changeStatusTo(OrderStatus targetStatus) {
        this.status = targetStatus;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<SubOrder> getSubOrders() {
        return Collections.unmodifiableList(subOrders);
    }
}
