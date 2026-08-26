package com.nexamarket.nexamarket.cart;

import com.nexamarket.nexamarket.cart.api.CartController;
import com.nexamarket.nexamarket.cart.application.CartApplicationService;
import com.nexamarket.nexamarket.cart.application.CartView;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartApplicationService cartApplicationService;

    @Test
    void addsAnItemAndReturnsCreated() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        when(cartApplicationService.addItem(any())).thenReturn(new CartView(cartId, CartStatus.ACTIVE, List.of()));

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "productVariantId": "%s",
                                  "sellerId": "%s",
                                  "quantity": 2
                                }
                                """.formatted(customerId, variantId, sellerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
