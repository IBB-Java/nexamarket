package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusServiceTest {

    @Mock
    private SubOrderRepository subOrderRepository;
    @Mock
    private OrderStatusEventPublisher orderStatusEventPublisher;

    @Test
    void updatesAnExistingSubOrderThroughTheStateMachine() {
        SubOrder subOrder = paymentPendingSubOrder();
        UUID subOrderId = UUID.randomUUID();
        OrderStatusService service = new OrderStatusService(subOrderRepository, orderStatusEventPublisher);
        when(subOrderRepository.findByIdForUpdate(subOrderId)).thenReturn(Optional.of(subOrder));
        when(subOrderRepository.save(any(SubOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderStatus response = service.updateSubOrderStatus(subOrderId, OrderStatus.PAID);

        assertThat(response).isEqualTo(OrderStatus.PAID);
        verify(subOrderRepository).save(subOrder);
        verify(orderStatusEventPublisher).enqueue(any(OrderStatusChangedEvent.class));
    }

    private SubOrder paymentPendingSubOrder() {
        CheckoutOrderRequest request = new CheckoutOrderRequest(
                420L,
                UUID.randomUUID(),
                List.of(new CheckoutOrderRequest.SellerOrderRequest(421L, List.of(
                        new CheckoutOrderRequest.OrderItemRequest(
                                422L,
                                1,
                                new BigDecimal("19.90"),
                                "reservation-422",
                                Instant.parse("2026-08-26T12:10:00Z"))))));
        return CustomerOrder.from(request).getSubOrders().getFirst();
    }
}
