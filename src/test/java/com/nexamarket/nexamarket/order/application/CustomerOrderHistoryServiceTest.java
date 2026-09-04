package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.order.api.CustomerOrderSummaryResponse;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import com.nexamarket.auth.entity.UserRole;
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
    @Mock
    private SubOrderRepository subOrderRepository;

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

    @Test
    void filtersSellerAndCourierViewsToTheirOwnSubOrders() {
        CustomerOrder order = CustomerOrder.from(new CheckoutOrderRequest(
                UUID.randomUUID(), 901L, List.of(
                new CheckoutOrderRequest.SellerOrderRequest(902L, List.of(
                        new CheckoutOrderRequest.OrderItemRequest(903L, 1, new BigDecimal("249.90"),
                                "reservation-903", Instant.parse("2026-08-31T08:00:00Z")))),
                new CheckoutOrderRequest.SellerOrderRequest(904L, List.of(
                        new CheckoutOrderRequest.OrderItemRequest(905L, 1, new BigDecimal("99.90"),
                                "reservation-905", Instant.parse("2026-08-31T08:00:00Z"))))),
                BigDecimal.ZERO, List.of(), "buyer@nexamarket.test"));
        order.getSubOrders().getFirst().assignCourier(906L);
        when(subOrderRepository.findBySellerIdWithOrder(902L)).thenReturn(List.of(order.getSubOrders().getFirst()));
        when(subOrderRepository.findByCourierIdWithOrder(906L)).thenReturn(List.of(order.getSubOrders().getFirst()));
        CustomerOrderHistoryService service = new CustomerOrderHistoryService(
                customerOrderRepository, subOrderRepository);

        List<CustomerOrderSummaryResponse> sellerView = service.listVisibleFor(902L, UserRole.SELLER);
        List<CustomerOrderSummaryResponse> courierView = service.listVisibleFor(906L, UserRole.COURIER);

        assertThat(sellerView).singleElement().satisfies(summary -> {
            assertThat(summary.customerEmail()).isEqualTo("buyer@nexamarket.test");
            assertThat(summary.subOrders()).hasSize(1);
            assertThat(summary.subOrders().getFirst().sellerId()).isEqualTo(902L);
        });
        assertThat(courierView).singleElement().satisfies(summary ->
                assertThat(summary.subOrders().getFirst().courierId()).isEqualTo(906L));
    }
}
