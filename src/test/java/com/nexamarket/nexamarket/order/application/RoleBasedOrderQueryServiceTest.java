package com.nexamarket.nexamarket.order.application;

import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.DeliveryAssignment;
import com.nexamarket.nexamarket.order.infrastructure.DeliveryAssignmentRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleBasedOrderQueryServiceTest {

    @Mock
    private SubOrderRepository subOrderRepository;
    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Test
    void routesEachRoleToItsOwnOrderScopeAndHidesCustomerIdentityFromSeller() {
        CustomerOrder order = order(11L, 22L);
        var subOrder = order.getSubOrders().getFirst();
        subOrder.assignCourier(33L);
        var assignment = DeliveryAssignment.assign(subOrder, 33L, Instant.parse("2026-08-26T12:20:00Z"));
        when(subOrderRepository.findByCustomerIdWithOrder(11L)).thenReturn(List.of(subOrder));
        when(subOrderRepository.findBySellerIdWithOrder(22L)).thenReturn(List.of(subOrder));
        when(deliveryAssignmentRepository.findByCourierIdWithOrder(33L)).thenReturn(List.of(assignment));
        when(subOrderRepository.findAllWithOrderOrderByCreatedAtDesc()).thenReturn(List.of(subOrder));
        RoleBasedOrderQueryService service = new RoleBasedOrderQueryService(
                subOrderRepository, deliveryAssignmentRepository);

        var customerRows = service.listVisibleOrders(principal(11L, UserRole.CUSTOMER));
        var sellerRows = service.listVisibleOrders(principal(22L, UserRole.SELLER));
        var courierRows = service.listVisibleOrders(principal(33L, UserRole.COURIER));
        var adminRows = service.listVisibleOrders(principal(44L, UserRole.ADMIN));

        assertThat(customerRows.getFirst().customerId()).isEqualTo(11L);
        assertThat(sellerRows.getFirst().customerId()).isNull();
        assertThat(courierRows.getFirst().courierId()).isEqualTo(33L);
        assertThat(adminRows.getFirst().customerId()).isEqualTo(11L);
        verify(subOrderRepository).findByCustomerIdWithOrder(11L);
        verify(subOrderRepository).findBySellerIdWithOrder(22L);
        verify(deliveryAssignmentRepository).findByCourierIdWithOrder(33L);
        verify(subOrderRepository).findAllWithOrderOrderByCreatedAtDesc();
    }

    private AuthPrincipal principal(Long userId, UserRole role) {
        return new AuthPrincipal(userId, role.name().toLowerCase() + "@nexamarket.test", role);
    }

    private CustomerOrder order(Long customerId, Long sellerId) {
        return CustomerOrder.from(new CheckoutOrderRequest(
                customerId,
                UUID.randomUUID(),
                List.of(new CheckoutOrderRequest.SellerOrderRequest(sellerId, List.of(
                        new CheckoutOrderRequest.OrderItemRequest(
                                99L, 2, new BigDecimal("15.00"), "reservation-99",
                                Instant.parse("2026-08-26T12:10:00Z")))))));
    }
}
