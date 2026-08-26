package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.cart.application.OrderCreation;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreationServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Test
    void createsParentOrderAndGroupsSubOrdersBySeller() {
        CheckoutOrderRequest request = checkoutRequest();
        OrderCreationService service = new OrderCreationService(customerOrderRepository);
        when(customerOrderRepository.findBySourceCartId(request.sourceCartId())).thenReturn(Optional.empty());
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderCreation response = service.createFromCart(request);

        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(customerOrderRepository).save(orderCaptor.capture());
        CustomerOrder order = orderCaptor.getValue();
        assertThat(response.orderId()).isEqualTo(order.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("46.00");
        assertThat(order.getSubOrders()).hasSize(2);
        assertThat(order.getSubOrders())
                .anySatisfy(subOrder -> assertThat(subOrder.getSubtotal()).isEqualByComparingTo("25.00"))
                .anySatisfy(subOrder -> assertThat(subOrder.getSubtotal()).isEqualByComparingTo("21.00"));
    }

    @Test
    void returnsExistingOrderWhenTheSameCartIsRetried() {
        CheckoutOrderRequest request = checkoutRequest();
        CustomerOrder existingOrder = CustomerOrder.from(request);
        OrderCreationService service = new OrderCreationService(customerOrderRepository);
        when(customerOrderRepository.findBySourceCartId(request.sourceCartId())).thenReturn(Optional.of(existingOrder));

        OrderCreation response = service.createFromCart(request);

        assertThat(response.orderId()).isEqualTo(existingOrder.getId());
        verify(customerOrderRepository, never()).save(any());
    }

    private CheckoutOrderRequest checkoutRequest() {
        Instant reservedUntil = Instant.parse("2026-08-26T12:10:00Z");
        UUID firstSellerId = UUID.randomUUID();
        UUID secondSellerId = UUID.randomUUID();
        return new CheckoutOrderRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(
                        new CheckoutOrderRequest.SellerOrderRequest(firstSellerId, List.of(
                                item(2, "10.00", reservedUntil),
                                item(1, "5.00", reservedUntil))),
                        new CheckoutOrderRequest.SellerOrderRequest(secondSellerId, List.of(
                                item(3, "7.00", reservedUntil)))));
    }

    private CheckoutOrderRequest.OrderItemRequest item(int quantity, String unitPrice, Instant reservedUntil) {
        return new CheckoutOrderRequest.OrderItemRequest(
                UUID.randomUUID(), quantity, new BigDecimal(unitPrice), UUID.randomUUID(), reservedUntil);
    }
}
