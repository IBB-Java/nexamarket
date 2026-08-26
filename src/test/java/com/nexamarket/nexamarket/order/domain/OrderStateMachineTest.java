package com.nexamarket.nexamarket.order.domain;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    private final OrderStateMachine stateMachine = new OrderStateMachine();

    @Test
    void allowsTheNormalOrderFulfillmentFlow() {
        SubOrder subOrder = paymentPendingSubOrder();

        stateMachine.transition(subOrder, OrderStatus.PAID);
        stateMachine.transition(subOrder, OrderStatus.PROCESSING);
        stateMachine.transition(subOrder, OrderStatus.SHIPPED);
        stateMachine.transition(subOrder, OrderStatus.DELIVERED);

        assertThat(subOrder.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void rejectsCancellationAfterTheOrderHasShipped() {
        SubOrder subOrder = paymentPendingSubOrder();
        stateMachine.transition(subOrder, OrderStatus.PAID);
        stateMachine.transition(subOrder, OrderStatus.PROCESSING);
        stateMachine.transition(subOrder, OrderStatus.SHIPPED);

        assertThatThrownBy(() -> stateMachine.transition(subOrder, OrderStatus.CANCELLED))
                .isInstanceOf(InvalidOrderStateTransitionException.class)
                .hasMessage("Transition from SHIPPED to CANCELLED is not allowed.");
        assertThat(subOrder.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void letsAShippedOrderEnterTheReturnFlow() {
        SubOrder subOrder = paymentPendingSubOrder();
        stateMachine.transition(subOrder, OrderStatus.PAID);
        stateMachine.transition(subOrder, OrderStatus.PROCESSING);
        stateMachine.transition(subOrder, OrderStatus.SHIPPED);

        stateMachine.transition(subOrder, OrderStatus.RETURN_REQUESTED);

        assertThat(subOrder.getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
    }

    private SubOrder paymentPendingSubOrder() {
        CheckoutOrderRequest request = new CheckoutOrderRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(new CheckoutOrderRequest.SellerOrderRequest(UUID.randomUUID(), List.of(
                        new CheckoutOrderRequest.OrderItemRequest(
                                UUID.randomUUID(),
                                1,
                                new BigDecimal("19.90"),
                                UUID.randomUUID(),
                                Instant.parse("2026-08-26T12:10:00Z"))))));
        return CustomerOrder.from(request).getSubOrders().getFirst();
    }
}
