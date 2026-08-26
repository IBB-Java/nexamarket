package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.application.OrderStatusService;
import com.nexamarket.nexamarket.order.domain.InvalidOrderStateTransitionException;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OrderController.class, OrderExceptionHandler.class})
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderStatusService orderStatusService;

    @Test
    void returnsConflictForAnInvalidStateTransition() throws Exception {
        UUID subOrderId = UUID.randomUUID();
        when(orderStatusService.updateSubOrderStatus(subOrderId, OrderStatus.CANCELLED))
                .thenThrow(new InvalidOrderStateTransitionException(OrderStatus.SHIPPED, OrderStatus.CANCELLED));

        mockMvc.perform(patch("/api/v1/orders/{subOrderId}/status", subOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid order status transition"))
                .andExpect(jsonPath("$.detail").value("Transition from SHIPPED to CANCELLED is not allowed."));
    }
}
