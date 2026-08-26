package com.nexamarket.nexamarket.order.domain;

public class InvalidOrderStateTransitionException extends RuntimeException {

    public InvalidOrderStateTransitionException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super("Transition from " + currentStatus + " to " + targetStatus + " is not allowed.");
    }
}
