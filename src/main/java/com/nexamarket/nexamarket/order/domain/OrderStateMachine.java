package com.nexamarket.nexamarket.order.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Central State-pattern implementation for order and sub-order transitions.
 * Keeping rules here prevents controllers and services from growing if/else chains.
 */
public class OrderStateMachine {

    private final Map<OrderStatus, OrderState> states;

    public OrderStateMachine() {
        this.states = new EnumMap<>(OrderStatus.class);
        states.put(OrderStatus.CREATED, state(OrderStatus.CREATED, OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED));
        states.put(OrderStatus.PAYMENT_PENDING, state(OrderStatus.PAYMENT_PENDING, OrderStatus.PAID, OrderStatus.CANCELLED));
        states.put(OrderStatus.PAID, state(OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.CANCELLED,
                OrderStatus.RETURN_REQUESTED));
        states.put(OrderStatus.PROCESSING, state(OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.CANCELLED,
                OrderStatus.RETURN_REQUESTED));
        states.put(OrderStatus.SHIPPED, state(OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED));
        states.put(OrderStatus.DELIVERED, state(OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED));
        states.put(OrderStatus.RETURN_REQUESTED, state(OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
                OrderStatus.RETURN_REJECTED));
        states.put(OrderStatus.RETURN_REJECTED, state(OrderStatus.RETURN_REJECTED, OrderStatus.DELIVERED));
        states.put(OrderStatus.CANCELLED, state(OrderStatus.CANCELLED));
        states.put(OrderStatus.RETURN_APPROVED, state(OrderStatus.RETURN_APPROVED));
    }

    public void transition(SubOrder subOrder, OrderStatus targetStatus) {
        stateFor(subOrder.getStatus()).validateTransitionTo(targetStatus);
        subOrder.changeStatusTo(targetStatus);
    }

    private OrderState stateFor(OrderStatus currentStatus) {
        return states.get(currentStatus);
    }

    private OrderState state(OrderStatus status, OrderStatus... allowedTargets) {
        return new OrderState(status, Set.of(allowedTargets));
    }
}
