package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentTimeoutServiceTest {

    private final Instant now = Instant.parse("2026-08-26T12:00:00Z");

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private StockReservationReleaseGateway stockReservationReleaseGateway;

    @Test
    void releasesReservationsAndCancelsTheTimedOutOrder() {
        CustomerOrder order = paymentPendingOrder();
        UUID firstReservationId = order.getSubOrders().getFirst().getItems().getFirst().getStockReservationId();
        UUID secondReservationId = order.getSubOrders().get(1).getItems().getFirst().getStockReservationId();
        OrderPaymentTimeoutService service = service();
        Instant createdBefore = now.minus(Duration.ofMinutes(10));
        when(customerOrderRepository.findByStatusAndCreatedAtBeforeForUpdate(OrderStatus.PAYMENT_PENDING, createdBefore))
                .thenReturn(List.of(order));

        int cancelledOrderCount = service.cancelTimedOutOrders();

        assertThat(cancelledOrderCount).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getSubOrders()).allSatisfy(subOrder ->
                assertThat(subOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED));
        verify(stockReservationReleaseGateway).releaseReservation(firstReservationId);
        verify(stockReservationReleaseGateway).releaseReservation(secondReservationId);
    }

    @Test
    void leavesTheOrderPendingWhenReservationReleaseFails() {
        CustomerOrder order = paymentPendingOrder();
        UUID reservationId = order.getSubOrders().getFirst().getItems().getFirst().getStockReservationId();
        OrderPaymentTimeoutService service = service();
        Instant createdBefore = now.minus(Duration.ofMinutes(10));
        when(customerOrderRepository.findByStatusAndCreatedAtBeforeForUpdate(OrderStatus.PAYMENT_PENDING, createdBefore))
                .thenReturn(List.of(order));
        doThrow(new IllegalStateException("Catalog service is unavailable"))
                .when(stockReservationReleaseGateway).releaseReservation(reservationId);

        assertThatThrownBy(service::cancelTimedOutOrders)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Catalog service is unavailable");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(order.getSubOrders()).allSatisfy(subOrder ->
                assertThat(subOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING));
    }

    private OrderPaymentTimeoutService service() {
        return new OrderPaymentTimeoutService(
                customerOrderRepository,
                stockReservationReleaseGateway,
                Duration.ofMinutes(10),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private CustomerOrder paymentPendingOrder() {
        Instant reservedUntil = now.plus(Duration.ofMinutes(10));
        CheckoutOrderRequest request = new CheckoutOrderRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new CheckoutOrderRequest.SellerOrderRequest(UUID.randomUUID(), List.of(item(reservedUntil))),
                        new CheckoutOrderRequest.SellerOrderRequest(UUID.randomUUID(), List.of(item(reservedUntil)))));
        return CustomerOrder.from(request);
    }

    private CheckoutOrderRequest.OrderItemRequest item(Instant reservedUntil) {
        return new CheckoutOrderRequest.OrderItemRequest(
                UUID.randomUUID(), 1, new BigDecimal("19.90"), UUID.randomUUID(), reservedUntil);
    }
}
