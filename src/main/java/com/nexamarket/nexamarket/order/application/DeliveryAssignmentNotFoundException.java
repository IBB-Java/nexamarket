package com.nexamarket.nexamarket.order.application;

import java.util.UUID;

public class DeliveryAssignmentNotFoundException extends RuntimeException {
    public DeliveryAssignmentNotFoundException(UUID assignmentId) {
        super("Teslimat ataması bulunamadı: " + assignmentId);
    }
}
