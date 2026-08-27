package com.nexamarket.nexamarket.cart;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import com.nexamarket.nexamarket.cart.infrastructure.CartRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartRepositoryIntegrationTest {

    @Autowired
    private CartRepository cartRepository;

    @Test
    void savesAnActiveCartWithItsItems() {
        Long customerId = 101L;
        Cart cart = new Cart(customerId);
        cart.addItem(
                102L,
                103L,
                2,
                new BigDecimal("499.90"),
                "reservation-102",
                Instant.parse("2026-08-26T12:00:00Z"));

        Cart savedCart = cartRepository.saveAndFlush(cart);

        assertThat(savedCart.getId()).isNotNull();
        assertThat(savedCart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(savedCart.getItems()).hasSize(1);
        assertThat(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE))
                .contains(savedCart);
    }
}
