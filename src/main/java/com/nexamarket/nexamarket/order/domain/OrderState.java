package com.nexamarket.nexamarket.order.domain;

import java.util.Set;

/** A state object declares the only transitions allowed from one order status. */
public final class OrderState {

    private final OrderStatus status;
    private final Set<OrderStatus> allowedTargets;

    OrderState(OrderStatus status, Set<OrderStatus> allowedTargets) {
        this.status = status;
        this.allowedTargets = Set.copyOf(allowedTargets);
    }

    public void validateTransitionTo(OrderStatus targetStatus) {
        if (!allowedTargets.contains(targetStatus)) {
            throw new InvalidOrderStateTransitionException(status, targetStatus);
        }
    }
}
