package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.order.api.CustomerOrderSummaryResponse;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
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
class CustomerOrderHistoryServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Test
    void returnsOnlyTheRequestedCustomersOrders() {
        CustomerOrder order = CustomerOrder.from(new CheckoutOrderRequest(
                UUID.randomUUID(), 901L, List.of(new CheckoutOrderRequest.SellerOrderRequest(902L, List.of(
                new CheckoutOrderRequest.OrderItemRequest(903L, 1, new BigDecimal("249.90"),
                        "reservation-903", Instant.parse("2026-08-31T08:00:00Z")))))));
        when(customerOrderRepository.findByCustomerIdOrderByCreatedAtDesc(901L)).thenReturn(List.of(order));
        CustomerOrderHistoryService service = new CustomerOrderHistoryService(customerOrderRepository);

        List<CustomerOrderSummaryResponse> result = service.listForCustomer(901L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().orderId()).isEqualTo(order.getId());
        assertThat(result.getFirst().totalAmount()).isEqualByComparingTo("249.90");
    }
}
