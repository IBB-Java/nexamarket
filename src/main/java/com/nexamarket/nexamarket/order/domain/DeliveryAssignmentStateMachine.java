package com.nexamarket.nexamarket.order.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Order durumlarından bağımsız kurye-atama yaşam döngüsü. */
public class DeliveryAssignmentStateMachine {

    private final Map<DeliveryAssignmentStatus, Set<DeliveryAssignmentStatus>> transitions =
            new EnumMap<>(DeliveryAssignmentStatus.class);

    public DeliveryAssignmentStateMachine() {
        transitions.put(DeliveryAssignmentStatus.ASSIGNED,
                Set.of(DeliveryAssignmentStatus.ACCEPTED, DeliveryAssignmentStatus.REJECTED));
        transitions.put(DeliveryAssignmentStatus.ACCEPTED,
                Set.of(DeliveryAssignmentStatus.PICKED_UP, DeliveryAssignmentStatus.DELIVERY_FAILED));
        transitions.put(DeliveryAssignmentStatus.PICKED_UP,
                Set.of(DeliveryAssignmentStatus.IN_TRANSIT, DeliveryAssignmentStatus.DELIVERY_FAILED));
        transitions.put(DeliveryAssignmentStatus.IN_TRANSIT,
                Set.of(DeliveryAssignmentStatus.DELIVERED, DeliveryAssignmentStatus.DELIVERY_FAILED));
        transitions.put(DeliveryAssignmentStatus.REJECTED, Set.of());
        transitions.put(DeliveryAssignmentStatus.DELIVERED, Set.of());
        transitions.put(DeliveryAssignmentStatus.DELIVERY_FAILED, Set.of());
    }

    public void transition(DeliveryAssignment assignment, DeliveryAssignmentStatus target, Instant now,
                           DeliveryFailureReasonCode failureReasonCode, String detail) {
        DeliveryAssignmentStatus current = assignment.getStatus();
        if (!transitions.getOrDefault(current, Set.of()).contains(target)) {
            throw new InvalidDeliveryStateTransitionException(current, target);
        }
        assignment.applyTransition(target, now, failureReasonCode, detail);
    }
}
