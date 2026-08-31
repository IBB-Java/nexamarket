package com.nexamarket.catalog.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamarket.catalog.application.ProductIndexingService;
import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.entity.ProductStatus;
import com.nexamarket.catalog.repository.ProductRepository;
import com.nexamarket.catalog.search.ProductSearchDocument;
import com.nexamarket.catalog.search.ProductSearchGateway;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductSearchApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductSearchGateway searchGateway;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductIndexingService productIndexingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String sellerToken;

    @BeforeEach
    void indexFixtures() throws Exception {
        adminToken = createUserAndLogin("admin-search-" + System.nanoTime() + "@nexamarket.test", UserRole.ADMIN);
        sellerToken = createUserAndLogin("seller-search-" + System.nanoTime() + "@nexamarket.test", UserRole.SELLER);
        searchGateway.index(document(
                "9001", "Kablosuz Kulaklık", "Bluetooth ve ANC",
                ProductStatus.ACTIVE, List.of(501L), List.of("Elektronik"), 100.0, 150.0, 4.7));
        searchGateway.index(document(
                "9002", "Kablolu Kulaklık", "Stüdyo kulaklığı",
                ProductStatus.ACTIVE, List.of(501L), List.of("Elektronik"), 30.0, 45.0, 4.2));
        searchGateway.index(document(
                "9003", "Kablosuz Hoparlör", "Taşınabilir",
                ProductStatus.PASSIVE, List.of(501L), List.of("Elektronik"), 80.0, 90.0, 4.9));
    }

    @Test
    void combinesTextCategoryPriceAndSellerRatingFilters() throws Exception {
        mockMvc.perform(get("/api/v1/products/search")
                        .param("q", "kablosuz")
                        .param("categoryId", "501")
                        .param("minPrice", "120")
                        .param("maxPrice", "200")
                        .param("minSellerRating", "4.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(9001))
                .andExpect(jsonPath("$.items[0].name").value("Kablosuz Kulaklık"));
    }

    @Test
    void excludesPassiveProductsAndPaginatesResults() throws Exception {
        mockMvc.perform(get("/api/v1/products/search")
                        .param("categoryId", "501")
                        .param("size", "1")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void rejectsInvertedPriceRange() throws Exception {
        mockMvc.perform(get("/api/v1/products/search")
                        .param("minPrice", "500")
                        .param("maxPrice", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Geçersiz arama kriteri"));
    }

    @Test
    void reflectsProductUpdateInSearchWithinConfiguredConsistencyBound() throws Exception {
        long categoryId = createCategory("Arama Tutarlılığı");
        JsonNode productJson = createProduct(categoryId);
        long productId = productJson.get("id").asLong();
        long variantId = productJson.get("variants").get(0).get("id").asLong();

        Product product = productRepository.findDetailedById(productId).orElseThrow();
        product.setStatus(ProductStatus.ACTIVE);
        productRepository.saveAndFlush(product);
        productIndexingService.index(productId);

        mockMvc.perform(patch("/api/v1/products/{productId}", productId)
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "FR-CAT-04 indeks yenilemesi",
                                  "basePrice": 199.99,
                                  "variants": [
                                    {"id": %d, "price": 219.99, "stockQuantity": 0}
                                  ]
                                }
                                """.formatted(variantId)))
                .andExpect(status().isOk());

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        AssertionError lastFailure = null;
        while (System.nanoTime() < deadline) {
            try {
                mockMvc.perform(get("/api/v1/products/search")
                                .param("q", "FR-CAT-04"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.items[0].id").value(productId))
                        .andExpect(jsonPath("$.items[0].minPrice").value(219.99))
                        .andExpect(jsonPath("$.items[0].maxPrice").value(219.99))
                        .andExpect(jsonPath("$.items[0].totalStock").value(0))
                        .andExpect(jsonPath("$.items[0].inStock").value(false));
                return;
            } catch (AssertionError assertionError) {
                lastFailure = assertionError;
                Thread.sleep(25);
            }
        }
        throw lastFailure == null ? new AssertionError("Arama indeksi zamanında güncellenmedi") : lastFailure;
    }

    private long createCategory(String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private JsonNode createProduct(long categoryId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tutarlılık Test Ürünü",
                                  "description": "Eski açıklama",
                                  "basePrice": 100.00,
                                  "categoryIds": [%d],
                                  "variants": [
                                    {"sku": "FR-CAT-04-SKU", "attributes": {}, "price": 120.00, "stockQuantity": 7}
                                  ]
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String createUserAndLogin(String email, UserRole role) throws Exception {
        userAccountRepository.save(UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("StrongPass!2026"))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass!2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private ProductSearchDocument document(
            String id,
            String name,
            String description,
            ProductStatus status,
            List<Long> categoryIds,
            List<String> categoryNames,
            double minPrice,
            double maxPrice,
            double rating
    ) {
        return ProductSearchDocument.builder()
                .id(id)
                .sellerId(700L)
                .name(name)
                .description(description)
                .status(status.name())
                .categoryIds(categoryIds)
                .categoryNames(categoryNames)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .sellerRating(rating)
                .build();
    }
}
