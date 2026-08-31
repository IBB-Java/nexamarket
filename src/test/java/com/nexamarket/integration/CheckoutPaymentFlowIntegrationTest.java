package com.nexamarket.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.UserAccountRepository;
import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.entity.ProductStatus;
import com.nexamarket.catalog.entity.ProductVariant;
import com.nexamarket.catalog.repository.ProductRepository;
import com.nexamarket.catalog.repository.ProductVariantRepository;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import com.nexamarket.nexamarket.payment.application.WalletService;
import com.nexamarket.stock.entity.StockReservationStatus;
import com.nexamarket.stock.repository.StockReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CheckoutPaymentFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private CustomerOrderRepository customerOrderRepository;
    @Autowired private StockReservationRepository stockReservationRepository;
    @Autowired private WalletService walletService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void customerCanReserveStockCheckoutAndPayForTheirOwnOrder() throws Exception {
        UserAccount seller = userAccountRepository.save(UserAccount.builder()
                .email("seller-flow@nexamarket.test")
                .passwordHash(passwordEncoder.encode("StrongPass!2026"))
                .role(UserRole.SELLER).status(UserStatus.ACTIVE).build());
        Product product = productRepository.save(Product.builder()
                .name("Akıllı Saat").description("Uçtan uca akış testi")
                .basePrice(new BigDecimal("20.00")).sellerId(seller.getId()).status(ProductStatus.ACTIVE).build());
        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(product).sku("FLOW-WATCH-01").price(new BigDecimal("20.00")).stockQuantity(5).build());

        String customerToken = registerAndLogin("customer-flow@nexamarket.test");
        UserAccount customer = userAccountRepository.findByEmailIgnoreCase("customer-flow@nexamarket.test").orElseThrow();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productVariantId\":" + variant.getId() + ",\"quantity\":2}"))
                .andExpect(status().isCreated());

        String checkoutBody = mockMvc.perform(post("/api/v1/cart/items/checkout")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID orderId = UUID.fromString(objectMapper.readTree(checkoutBody).get("orderId").asText());

        CustomerOrder order = customerOrderRepository.findByIdWithSubOrdersForUpdate(orderId).orElseThrow();
        assertThat(order.getCustomerId()).isEqualTo(customer.getId());
        assertThat(order.getSubOrders()).singleElement().satisfies(subOrder -> {
            assertThat(subOrder.getSellerId()).isEqualTo(seller.getId());
            assertThat(subOrder.getItems()).hasSize(1);
        });
        assertThat(stockReservationRepository.findAll()).singleElement()
                .extracting(reservation -> reservation.getStatus())
                .isEqualTo(StockReservationStatus.ACTIVE);

        walletService.credit(customer.getId(), new BigDecimal("40.00"));
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","idempotencyKey":"flow-payment-001","walletAmount":40.00,"cardAmount":0.00}
                                """.formatted(orderId)))
                .andExpect(status().isCreated());

        assertThat(customerOrderRepository.findByIdWithSubOrdersForUpdate(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PAID);
        assertThat(stockReservationRepository.findAll()).singleElement()
                .extracting(reservation -> reservation.getStatus())
                .isEqualTo(StockReservationStatus.CONFIRMED);
        assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
    }

    @Test
    void sellerCannotAddToCartOrCheckout() throws Exception {
        UserAccount seller = userAccountRepository.save(UserAccount.builder()
                .email("seller-no-shopping@nexamarket.test")
                .passwordHash(passwordEncoder.encode("StrongPass!2026"))
                .role(UserRole.SELLER).status(UserStatus.ACTIVE).build());
        Product product = productRepository.save(Product.builder()
                .name("Satıcıya Kapalı Ürün").description("Rol testi")
                .basePrice(new BigDecimal("20.00")).sellerId(seller.getId()).status(ProductStatus.ACTIVE).build());
        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(product).sku("NO-SHOPPING-01").price(new BigDecimal("20.00")).stockQuantity(5).build());

        String sellerToken = login("seller-no-shopping@nexamarket.test");

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productVariantId\":" + variant.getId() + ",\"quantity\":1}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/cart/items/checkout")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isForbidden());
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass!2026\"}"))
                .andExpect(status().isCreated());
        return login(email);
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass!2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode tokens = objectMapper.readTree(response);
        return tokens.get("accessToken").asText();
    }
}
