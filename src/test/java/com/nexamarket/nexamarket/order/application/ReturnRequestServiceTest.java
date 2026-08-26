package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStateMachine;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.ReturnRequest;
import com.nexamarket.nexamarket.order.domain.ReturnRequestStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.ReturnRequestRepository;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnRequestServiceTest {

    @Mock
    private SubOrderRepository subOrderRepository;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Test
    void createsARequestedReturnAndMovesTheSubOrderIntoTheReturnFlow() {
        SubOrder subOrder = shippedSubOrder();
        UUID subOrderId = UUID.randomUUID();
        ReturnRequestService service = service();
        when(subOrderRepository.findByIdForUpdate(subOrderId)).thenReturn(Optional.of(subOrder));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnRequest returnRequest = service.create(new CreateReturnRequestCommand(subOrderId, "Damaged on delivery"));

        assertThat(returnRequest.getStatus()).isEqualTo(ReturnRequestStatus.REQUESTED);
        assertThat(subOrder.getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
    }

    @Test
    void approvesARequestedReturnAndUpdatesTheSubOrderStatus() {
        SubOrder subOrder = shippedSubOrder();
        OrderStateMachine stateMachine = new OrderStateMachine();
        stateMachine.transition(subOrder, OrderStatus.RETURN_REQUESTED);
        ReturnRequest returnRequest = new ReturnRequest(subOrder, "Damaged on delivery");
        UUID returnRequestId = UUID.randomUUID();
        UUID resolverId = UUID.randomUUID();
        ReturnRequestService service = service();
        when(returnRequestRepository.findByIdForUpdate(returnRequestId)).thenReturn(Optional.of(returnRequest));
        when(returnRequestRepository.save(returnRequest)).thenReturn(returnRequest);

        ReturnRequest resolved = service.resolve(new ResolveReturnRequestCommand(
                returnRequestId, ReturnRequestStatus.APPROVED, resolverId));

        assertThat(resolved.getStatus()).isEqualTo(ReturnRequestStatus.APPROVED);
        assertThat(subOrder.getStatus()).isEqualTo(OrderStatus.RETURN_APPROVED);
        verify(returnRequestRepository).save(returnRequest);
    }

    private ReturnRequestService service() {
        return new ReturnRequestService(
                subOrderRepository,
                returnRequestRepository,
                Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneOffset.UTC));
    }

    private SubOrder shippedSubOrder() {
        CustomerOrder order = CustomerOrder.from(new CheckoutOrderRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(new CheckoutOrderRequest.SellerOrderRequest(UUID.randomUUID(), List.of(
                        new CheckoutOrderRequest.OrderItemRequest(
                                UUID.randomUUID(), 1, new BigDecimal("19.90"), UUID.randomUUID(),
                                Instant.parse("2026-08-26T12:10:00Z")))))));
        SubOrder subOrder = order.getSubOrders().getFirst();
        OrderStateMachine stateMachine = new OrderStateMachine();
        stateMachine.transition(subOrder, OrderStatus.PAID);
        stateMachine.transition(subOrder, OrderStatus.PROCESSING);
        stateMachine.transition(subOrder, OrderStatus.SHIPPED);
        return subOrder;
    }
}
