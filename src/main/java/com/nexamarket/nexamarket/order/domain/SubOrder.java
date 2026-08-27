package com.nexamarket.nexamarket.order.domain;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
@Table(name = "sub_orders")
public class SubOrder {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private CustomerOrder order;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "subOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderItem> items = new ArrayList<>();

    protected SubOrder() {
    }

    private SubOrder(CustomerOrder order, Long sellerId) {
        this.id = UUID.randomUUID();
        this.order = Objects.requireNonNull(order, "Order is required.");
        this.sellerId = Objects.requireNonNull(sellerId, "Seller id is required.");
        this.status = OrderStatus.PAYMENT_PENDING;
        this.subtotal = BigDecimal.ZERO;
    }

    static SubOrder from(CustomerOrder order, CheckoutOrderRequest.SellerOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("A seller order must contain at least one item.");
        }
        SubOrder subOrder = new SubOrder(order, request.sellerId());
        for (CheckoutOrderRequest.OrderItemRequest itemRequest : request.items()) {
            OrderItem item = OrderItem.from(subOrder, itemRequest);
            subOrder.items.add(item);
            subOrder.subtotal = subOrder.subtotal.add(item.getLineTotal());
        }
        return subOrder;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public UUID getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public OrderStatus getStatus() {
        return status;
    }

    void changeStatusTo(OrderStatus targetStatus) {
        this.status = targetStatus;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
