package com.nexamarket.nexamarket.order.domain;

public class InvalidDeliveryStateTransitionException extends RuntimeException {

    public InvalidDeliveryStateTransitionException(DeliveryAssignmentStatus current,
                                                    DeliveryAssignmentStatus target) {
        super("Geçersiz teslimat geçişi: " + current + " -> " + target);
    }
}
