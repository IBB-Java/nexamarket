package com.nexamarket.nexamarket.order.application;

import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.UserAccountRepository;
import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.order.api.CourierOrderResponse;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourierAssignmentServiceTest {

    @Mock
    private SubOrderRepository subOrderRepository;
    @Mock
    private UserAccountRepository userAccountRepository;

    @Test
    void adminCanAssignAnActiveCourierToASubOrder() {
        UserAccount courier = mock(UserAccount.class);
        when(courier.getId()).thenReturn(700L);
        when(courier.getRole()).thenReturn(UserRole.COURIER);
        when(courier.getStatus()).thenReturn(UserStatus.ACTIVE);
        SubOrder subOrder = paymentPendingSubOrder();
        when(userAccountRepository.findById(700L)).thenReturn(Optional.of(courier));
        when(subOrderRepository.findByIdForUpdate(subOrder.getId())).thenReturn(Optional.of(subOrder));
        when(subOrderRepository.save(any(SubOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CourierAssignmentService service = new CourierAssignmentService(subOrderRepository, userAccountRepository);

        CourierOrderResponse response = service.assign(subOrder.getId(), 700L);

        assertThat(response.courierId()).isEqualTo(700L);
        assertThat(subOrder.getCourierId()).isEqualTo(700L);
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
