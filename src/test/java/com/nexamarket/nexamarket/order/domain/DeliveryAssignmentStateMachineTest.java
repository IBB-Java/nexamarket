package com.nexamarket.nexamarket.order.domain;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryAssignmentStateMachineTest {

    private final DeliveryAssignmentStateMachine stateMachine = new DeliveryAssignmentStateMachine();
    private final Instant now = Instant.parse("2026-09-04T10:00:00Z");

    @Test
    void followsTheHappyPathAndRecordsMilestones() {
        DeliveryAssignment assignment = assignment();

        stateMachine.transition(assignment, DeliveryAssignmentStatus.ACCEPTED, now, null, null);
        stateMachine.transition(assignment, DeliveryAssignmentStatus.PICKED_UP, now.plusSeconds(10), null, null);
        stateMachine.transition(assignment, DeliveryAssignmentStatus.IN_TRANSIT, now.plusSeconds(20), null, null);
        stateMachine.transition(assignment, DeliveryAssignmentStatus.DELIVERED, now.plusSeconds(30), null, null);

        assertThat(assignment.getStatus()).isEqualTo(DeliveryAssignmentStatus.DELIVERED);
        assertThat(assignment.getAcceptedAt()).isEqualTo(now);
        assertThat(assignment.getPickedUpAt()).isEqualTo(now.plusSeconds(10));
        assertThat(assignment.getDeliveryStartedAt()).isEqualTo(now.plusSeconds(20));
        assertThat(assignment.getDeliveredAt()).isEqualTo(now.plusSeconds(30));
        assertThat(assignment.isActive()).isFalse();
    }

    @Test
    void rejectsInvalidDirectAndTerminalTransitions() {
        DeliveryAssignment assigned = assignment();
        assertThatThrownBy(() -> stateMachine.transition(
                assigned, DeliveryAssignmentStatus.DELIVERED, now, null, null))
                .isInstanceOf(InvalidDeliveryStateTransitionException.class);

        stateMachine.transition(assigned, DeliveryAssignmentStatus.REJECTED, now, null, "Çok uzak");
        assertThatThrownBy(() -> stateMachine.transition(
                assigned, DeliveryAssignmentStatus.ACCEPTED, now, null, null))
                .isInstanceOf(InvalidDeliveryStateTransitionException.class);
    }

    @Test
    void storesFailureReasonWithoutCancellingTheOrder() {
        DeliveryAssignment assignment = assignment();
        stateMachine.transition(assignment, DeliveryAssignmentStatus.ACCEPTED, now, null, null);
        stateMachine.transition(assignment, DeliveryAssignmentStatus.DELIVERY_FAILED, now.plusSeconds(5),
                DeliveryFailureReasonCode.VEHICLE_BREAKDOWN, "Araç arızası");

        assertThat(assignment.getFailureReasonCode()).isEqualTo(DeliveryFailureReasonCode.VEHICLE_BREAKDOWN);
        assertThat(assignment.getFailureDescription()).isEqualTo("Araç arızası");
        assertThat(assignment.getSubOrder().getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    private DeliveryAssignment assignment() {
        CustomerOrder order = CustomerOrder.from(new CheckoutOrderRequest(
                UUID.randomUUID(), 1L, List.of(new CheckoutOrderRequest.SellerOrderRequest(2L, List.of(
                new CheckoutOrderRequest.OrderItemRequest(3L, 1, new BigDecimal("10.00"),
                        UUID.randomUUID().toString(), now.plusSeconds(3600)))))));
        return DeliveryAssignment.assign(order.getSubOrders().getFirst(), 4L, now.minusSeconds(60));
    }
}
