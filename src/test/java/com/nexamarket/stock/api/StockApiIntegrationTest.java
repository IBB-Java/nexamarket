package com.nexamarket.stock.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamarket.auth.entity.RefreshToken;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.RefreshTokenRepository;
import com.nexamarket.auth.repository.UserAccountRepository;
import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.entity.ProductStatus;
import com.nexamarket.catalog.entity.ProductVariant;
import com.nexamarket.catalog.repository.ProductRepository;
import com.nexamarket.catalog.repository.ProductVariantRepository;
import com.nexamarket.stock.repository.StockReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        stockReservationRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void reservesAndReleasesStockThroughAuthenticatedApi() throws Exception {
        ProductVariant variant = createVariant(5L, 5);
        String customerToken = registerAndLogin("customer-stock@nexamarket.test", UserRole.CUSTOMER);

        mockMvc.perform(post("/api/v1/stocks/reservations")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":" + variant.getId() + ",\"quantity\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.availableStock").value(2))
                .andExpect(jsonPath("$.reservationCode").isNotEmpty());

        String reservationCode = stockReservationRepository.findAll().getFirst().getReservationCode();
        mockMvc.perform(delete("/api/v1/stocks/reservations/{reservationCode}", reservationCode)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"))
                .andExpect(jsonPath("$.availableStock").value(5));

        mockMvc.perform(get("/api/v1/stocks/variants/{variantId}", variant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableStock").value(5));
    }

    @Test
    void restrictsReservationAndStockManagementEndpoints() throws Exception {
        ProductVariant variant = createVariant(11L, 4);
        mockMvc.perform(post("/api/v1/stocks/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":" + variant.getId() + ",\"quantity\":1}"))
                .andExpect(status().isUnauthorized());

        String customerToken = registerAndLogin("customer-restricted@nexamarket.test", UserRole.CUSTOMER);
        mockMvc.perform(patch("/api/v1/stocks/variants/{variantId}", variant.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockQuantity\":20}"))
                .andExpect(status().isForbidden());

        String sellerToken = registerAndLogin("seller-stock@nexamarket.test", UserRole.SELLER);
        UserAccount seller = userAccountRepository.findByEmailIgnoreCase("seller-stock@nexamarket.test").orElseThrow();
        Product product = productRepository.findById(variant.getProduct().getId()).orElseThrow();
        product.setSellerId(seller.getId());
        productRepository.save(product);

        mockMvc.perform(patch("/api/v1/stocks/variants/{variantId}", variant.getId())
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockQuantity\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableStock").value(20));
    }

    private ProductVariant createVariant(Long sellerId, int stockQuantity) {
        Product product = productRepository.save(Product.builder()
                .name("Stok Test Ürünü")
                .description("Rezervasyon testi")
                .basePrice(BigDecimal.TEN)
                .sellerId(sellerId)
                .status(ProductStatus.ACTIVE)
                .build());
        return productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .sku("STOCK-" + sellerId + "-" + stockQuantity)
                .price(BigDecimal.TEN)
                .stockQuantity(stockQuantity)
                .build());
    }

    private String registerAndLogin(String email, UserRole role) throws Exception {
        if (role == UserRole.CUSTOMER) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass!2026\"}"))
                    .andExpect(status().isCreated());
        } else {
            userAccountRepository.save(UserAccount.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode("StrongPass!2026"))
                    .role(role)
                    .status(UserStatus.ACTIVE)
                    .build());
        }

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass!2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode tokens = objectMapper.readTree(response);
        return tokens.get("accessToken").asText();
    }
}
