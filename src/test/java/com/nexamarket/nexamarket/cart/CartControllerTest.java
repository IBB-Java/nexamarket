package com.nexamarket.nexamarket.cart;

import com.nexamarket.nexamarket.cart.api.CartController;
import com.nexamarket.common.audit.AuditLogRepository;
import com.nexamarket.nexamarket.cart.application.CartApplicationService;
import com.nexamarket.nexamarket.cart.application.CartCheckoutService;
import com.nexamarket.nexamarket.cart.application.CartView;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@ActiveProfiles("test")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartApplicationService cartApplicationService;

    @MockBean
    private CartCheckoutService cartCheckoutService;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @Test
    void addsAnItemAndReturnsCreated() throws Exception {
        UUID cartId = UUID.randomUUID();
        when(cartApplicationService.addItem(any())).thenReturn(new CartView(cartId, CartStatus.ACTIVE, List.of()));

        mockMvc.perform(post("/api/v1/cart/items")
                .with(authentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                new AuthPrincipal(1L, "cart@nexamarket.test", UserRole.CUSTOMER), null,
                                java.util.List.of())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productVariantId": 2,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
