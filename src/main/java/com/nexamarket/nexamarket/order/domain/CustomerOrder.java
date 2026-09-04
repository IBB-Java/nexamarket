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
    private Long customerId;

    @Column(name = "customer_email", nullable = false, length = 320, updatable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "promotion_codes", length = 500)
    private String promotionCodes;

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

    private CustomerOrder(UUID sourceCartId, Long customerId, String customerEmail) {
        this.id = UUID.randomUUID();
        this.sourceCartId = Objects.requireNonNull(sourceCartId, "Source cart id is required.");
        this.customerId = Objects.requireNonNull(customerId, "Customer id is required.");
        this.customerEmail = Objects.requireNonNull(customerEmail, "Customer email is required.");
        this.status = OrderStatus.PAYMENT_PENDING;
        this.subtotalAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }

    public static CustomerOrder from(CheckoutOrderRequest request) {
        Objects.requireNonNull(request, "Checkout request is required.");
        String customerEmail = request.customerEmail() == null || request.customerEmail().isBlank()
                ? "Silinmiş kullanıcı #" + request.customerId()
                : request.customerEmail();
        return from(request, customerEmail);
    }

    public static CustomerOrder from(CheckoutOrderRequest request, String customerEmail) {
        Objects.requireNonNull(request, "Checkout request is required.");
        if (request.sellerOrders() == null || request.sellerOrders().isEmpty()) {
            throw new IllegalArgumentException("At least one seller order is required.");
        }

        CustomerOrder order = new CustomerOrder(request.sourceCartId(), request.customerId(), customerEmail);
        for (CheckoutOrderRequest.SellerOrderRequest sellerOrderRequest : request.sellerOrders()) {
            SubOrder subOrder = SubOrder.from(order, sellerOrderRequest);
            order.subOrders.add(subOrder);
            order.subtotalAmount = order.subtotalAmount.add(subOrder.getSubtotal());
        }
        if (request.discountAmount().signum() < 0 || request.discountAmount().compareTo(order.subtotalAmount) > 0) {
            throw new IllegalArgumentException("Order discount is invalid.");
        }
        order.discountAmount = request.discountAmount();
        order.totalAmount = order.subtotalAmount.subtract(order.discountAmount);
        order.promotionCodes = String.join(",", request.promotionCodes());
        return order;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceCartId() {
        return sourceCartId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
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

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public String getPromotionCodes() {
        return promotionCodes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<SubOrder> getSubOrders() {
        return Collections.unmodifiableList(subOrders);
    }
}
