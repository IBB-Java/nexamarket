package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourierAssignmentServiceTest {

    @Mock
    private SubOrderRepository subOrderRepository;
    @Mock
    private CourierDirectoryGateway courierDirectoryGateway;

    @Test
    void assignsPaidSubOrdersToTheLeastBusyActiveCourier() {
        CustomerOrder order = CustomerOrder.from(new CheckoutOrderRequest(
                420L,
                UUID.randomUUID(),
                List.of(new CheckoutOrderRequest.SellerOrderRequest(421L, List.of(
                        item(422L))), new CheckoutOrderRequest.SellerOrderRequest(423L, List.of(item(424L))))));
        when(courierDirectoryGateway.findActiveCourierIds()).thenReturn(List.of(700L, 701L));
        when(subOrderRepository.countByCourierIdAndStatusIn(700L, java.util.EnumSet.of(
                OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.RETURN_REQUESTED))).thenReturn(3L);
        when(subOrderRepository.countByCourierIdAndStatusIn(701L, java.util.EnumSet.of(
                OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.RETURN_REQUESTED))).thenReturn(1L);

        new AutomaticCourierAssignmentService(courierDirectoryGateway, subOrderRepository).assignAfterPayment(order);

        assertThat(order.getSubOrders()).extracting(subOrder -> subOrder.getCourierId()).containsOnly(701L);
    }

    private CheckoutOrderRequest.OrderItemRequest item(Long variantId) {
        return new CheckoutOrderRequest.OrderItemRequest(
                variantId, 1, new BigDecimal("19.90"), "reservation-" + variantId,
                Instant.parse("2026-08-26T12:10:00Z"));
    }
}
