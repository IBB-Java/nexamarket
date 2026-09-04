package com.nexamarket.nexamarket.order.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.RefreshTokenRepository;
import com.nexamarket.auth.repository.UserAccountRepository;
import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.order.application.CourierDirectoryGateway;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStateMachine;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import com.nexamarket.nexamarket.order.infrastructure.DeliveryAssignmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleBasedOrderAccessApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private CustomerOrderRepository customerOrderRepository;
    @Autowired private DeliveryAssignmentRepository deliveryAssignmentRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockBean
    private CourierDirectoryGateway courierDirectoryGateway;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM notification_messages");
        jdbcTemplate.update("DELETE FROM notification_outbox_events");
        jdbcTemplate.update("DELETE FROM delivery_notification_outbox_events");
        jdbcTemplate.update("DELETE FROM loyalty_ledger_entries");
        deliveryAssignmentRepository.deleteAll();
        customerOrderRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void enforcesRoleScopesAndRunsTheCompleteManualDeliveryLifecycle() throws Exception {
        UserAccount firstCustomer = user(UserRole.CUSTOMER, "scope-customer-1@nexamarket.test");
        UserAccount secondCustomer = user(UserRole.CUSTOMER, "scope-customer-2@nexamarket.test");
        UserAccount firstSeller = user(UserRole.SELLER, "scope-seller-1@nexamarket.test");
        UserAccount secondSeller = user(UserRole.SELLER, "scope-seller-2@nexamarket.test");
        UserAccount firstCourier = user(UserRole.COURIER, "scope-courier-1@nexamarket.test");
        UserAccount secondCourier = user(UserRole.COURIER, "scope-courier-2@nexamarket.test");
        UserAccount admin = user(UserRole.ADMIN, "scope-admin@nexamarket.test");

        CustomerOrder firstOrder = paidOrder(firstCustomer.getId(), firstCustomer.getEmail(), firstSeller.getId(), "100.00");
        CustomerOrder secondOrder = paidOrder(secondCustomer.getId(), secondCustomer.getEmail(), secondSeller.getId(), "200.00");
        String customerToken = login(firstCustomer);
        String secondCustomerToken = login(secondCustomer);
        String sellerToken = login(firstSeller);
        String courierToken = login(firstCourier);
        String secondCourierToken = login(secondCourier);
        String adminToken = login(admin);

        // Paid orders stay unassigned until an administrator explicitly assigns a courier.
        mockMvc.perform(get("/api/v1/courier/deliveries").header("Authorization", bearer(courierToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/courier/deliveries").header("Authorization", bearer(secondCourierToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/courier/deliveries").header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/courier/deliveries").header("Authorization", bearer(sellerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/deliveries").header("Authorization", bearer(courierToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/products/search").header("Authorization", bearer(courierToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/orders").header("Authorization", bearer(courierToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/orders").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(firstOrder.getId().toString()));
        mockMvc.perform(get("/api/v1/orders/{orderId}", secondOrder.getId())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/orders").header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sellerId").value(firstSeller.getId()));
        mockMvc.perform(get("/api/v1/orders").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerEmail").value(secondCustomer.getEmail()))
                .andExpect(jsonPath("$[1].customerEmail").value(firstCustomer.getEmail()));

        // A courier must be active at assignment time.
        mockMvc.perform(post("/api/v1/admin/deliveries/assign")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subOrderId\":\"" + firstOrder.getSubOrders().getFirst().getId()
                                + "\",\"courierId\":" + firstCourier.getId() + "}"))
                .andExpect(status().isConflict());

        when(courierDirectoryGateway.isActiveCourier(firstCourier.getId())).thenReturn(true);
        when(courierDirectoryGateway.isActiveCourier(secondCourier.getId())).thenReturn(true);

        UUID firstAssignment = assign(adminToken, firstOrder, firstCourier.getId());
        // Application validation and the database uniqueness rule both prevent a second active assignment.
        mockMvc.perform(post("/api/v1/admin/deliveries/assign")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subOrderId\":\"" + firstOrder.getSubOrders().getFirst().getId()
                                + "\",\"courierId\":" + secondCourier.getId() + "}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/courier/deliveries").header("Authorization", bearer(courierToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].assignmentId").value(firstAssignment.toString()))
                .andExpect(jsonPath("$[0].customerId").doesNotExist())
                .andExpect(jsonPath("$[0].sellerId").doesNotExist())
                .andExpect(jsonPath("$[0].subtotal").doesNotExist());
        mockMvc.perform(get("/api/v1/courier/deliveries").header("Authorization", bearer(secondCourierToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/accept", firstAssignment)
                        .header("Authorization", bearer(secondCourierToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/accept", firstAssignment)
                        .header("Authorization", bearer(courierToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedAt").isNotEmpty());
        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/accept", firstAssignment)
                        .header("Authorization", bearer(courierToken)))
                .andExpect(status().isConflict());

        var firstSubOrder = firstOrder.getSubOrders().getFirst();
        mockMvc.perform(patch("/api/v1/orders/{id}/status", firstSubOrder.getId())
                        .header("Authorization", bearer(sellerToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/pickup", firstAssignment)
                        .header("Authorization", bearer(courierToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PICKED_UP"))
                .andExpect(jsonPath("$.orderStatus").value("SHIPPED"));
        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/start", firstAssignment)
                        .header("Authorization", bearer(courierToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_TRANSIT"));
        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/deliver", firstAssignment)
                        .header("Authorization", bearer(courierToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.orderStatus").value("DELIVERED"));
        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/accept", firstAssignment)
                        .header("Authorization", bearer(courierToken)))
                .andExpect(status().isConflict());

        UUID rejectedAssignment = assign(adminToken, secondOrder, firstCourier.getId());
        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/reject", rejectedAssignment)
                        .header("Authorization", bearer(courierToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/reject", rejectedAssignment)
                        .header("Authorization", bearer(courierToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Teslimat bölgesi çok uzak\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/accept", rejectedAssignment)
                        .header("Authorization", bearer(courierToken)))
                .andExpect(status().isConflict());

        UUID failedAssignment = assign(adminToken, secondOrder, secondCourier.getId());
        assertThat(failedAssignment).isNotEqualTo(rejectedAssignment);
        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/accept", failedAssignment)
                        .header("Authorization", bearer(secondCourierToken)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/fail", failedAssignment)
                        .header("Authorization", bearer(secondCourierToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/courier/deliveries/{id}/fail", failedAssignment)
                        .header("Authorization", bearer(secondCourierToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reasonCode\":\"VEHICLE_BREAKDOWN\",\"description\":\"Araç arızalandı\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERY_FAILED"))
                .andExpect(jsonPath("$.orderStatus").value("PAID"));

        UUID reassigned = assign(adminToken, secondOrder, firstCourier.getId());
        assertThat(reassigned).isNotIn(rejectedAssignment, failedAssignment);
        mockMvc.perform(get("/api/v1/admin/deliveries").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));

        long assignmentsBeforeDisable = deliveryAssignmentRepository.count();
        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", firstCustomer.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/orders").header("Authorization", bearer(customerToken)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/orders/{id}", firstOrder.getId())
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/courier/deliveries").header("Authorization", bearer(courierToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/orders/{id}", firstOrder.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerEmail").value(firstCustomer.getEmail()));
        assertThat(deliveryAssignmentRepository.count()).isEqualTo(assignmentsBeforeDisable);

        mockMvc.perform(post("/api/v1/admin/deliveries/assign")
                        .header("Authorization", bearer(secondCustomerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subOrderId\":\"" + secondOrder.getSubOrders().getFirst().getId()
                                + "\",\"courierId\":" + secondCourier.getId() + "}"))
                .andExpect(status().isForbidden());
    }

    private UUID assign(String adminToken, CustomerOrder order, Long courierId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/admin/deliveries/assign")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subOrderId\":\"" + order.getSubOrders().getFirst().getId()
                                + "\",\"courierId\":" + courierId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courierId").value(courierId))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("assignmentId").asText());
    }

    private UserAccount user(UserRole role, String email) {
        return userAccountRepository.save(UserAccount.builder()
                .email(email).passwordHash(passwordEncoder.encode("StrongPass!2026"))
                .role(role).status(UserStatus.ACTIVE).build());
    }

    private String login(UserAccount user) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"StrongPass!2026\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private CustomerOrder paidOrder(Long customerId, String customerEmail, Long sellerId, String unitPrice) {
        var item = new CheckoutOrderRequest.OrderItemRequest(
                999L, 1, new BigDecimal(unitPrice), UUID.randomUUID().toString(),
                Instant.parse("2026-09-04T08:00:00Z"));
        var sellerOrder = new CheckoutOrderRequest.SellerOrderRequest(sellerId, List.of(item));
        CustomerOrder order = CustomerOrder.from(new CheckoutOrderRequest(
                UUID.randomUUID(), customerId, List.of(sellerOrder), BigDecimal.ZERO, List.of(), customerEmail));
        OrderStateMachine stateMachine = new OrderStateMachine();
        order.getSubOrders().forEach(subOrder -> stateMachine.transition(subOrder, OrderStatus.PAID));
        stateMachine.transition(order, OrderStatus.PAID);
        return customerOrderRepository.save(order);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
