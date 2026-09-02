package com.nexamarket.nexamarket.order.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.repository.RefreshTokenRepository;
import com.nexamarket.auth.repository.UserAccountRepository;
import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerOrderHistoryApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CustomerOrderRepository customerOrderRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserAccountRepository userAccountRepository;

    @AfterEach
    void cleanUp() {
        customerOrderRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void returnsOnlyOrdersOwnedByTheAuthenticatedCustomer() throws Exception {
        String firstEmail = "first-history@nexamarket.test";
        String secondEmail = "second-history@nexamarket.test";
        String firstToken = registerAndLogin(firstEmail);
        String secondToken = registerAndLogin(secondEmail);
        UserAccount firstCustomer = userAccountRepository.findByEmailIgnoreCase(firstEmail).orElseThrow();
        UserAccount secondCustomer = userAccountRepository.findByEmailIgnoreCase(secondEmail).orElseThrow();
        CustomerOrder firstOrder = customerOrderRepository.save(orderFor(firstCustomer.getId(), "199.90"));
        CustomerOrder secondOrder = customerOrderRepository.save(orderFor(secondCustomer.getId(), "399.90"));

        mockMvc.perform(get("/api/v1/orders/me")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(firstOrder.getId().toString()))
                .andExpect(jsonPath("$[0].totalAmount").value(199.90))
                .andExpect(jsonPath("$[0].subOrders.length()").value(1))
                .andExpect(jsonPath("$[0].subOrders[0].subOrderId")
                        .value(firstOrder.getSubOrders().getFirst().getId().toString()))
                .andExpect(jsonPath("$[0].subOrders[0].itemCount").value(1));

        mockMvc.perform(get("/api/v1/orders/me")
                        .header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(secondOrder.getId().toString()))
                .andExpect(jsonPath("$[0].totalAmount").value(399.90));
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass!2026\"}"))
                .andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass!2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode tokens = objectMapper.readTree(response);
        return tokens.get("accessToken").asText();
    }

    private CustomerOrder orderFor(Long customerId, String total) {
        return CustomerOrder.from(new CheckoutOrderRequest(
                UUID.randomUUID(), customerId, List.of(new CheckoutOrderRequest.SellerOrderRequest(400L, List.of(
                new CheckoutOrderRequest.OrderItemRequest(500L, 1, new BigDecimal(total),
                        UUID.randomUUID().toString(), Instant.parse("2026-08-31T08:00:00Z")))))));
    }
}
