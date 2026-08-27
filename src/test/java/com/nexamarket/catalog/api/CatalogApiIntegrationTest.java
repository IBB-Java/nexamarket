package com.nexamarket.catalog.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsCategoryAndSellerProduct() throws Exception {
        long categoryId = createCategory("Elektronik");

        String productJson = """
                {
                  "name": "Kablosuz Kulaklık",
                  "description": "Aktif gürültü engelleme",
                  "basePrice": 2500.00,
                  "categoryIds": [%d],
                  "variants": [
                    {
                      "sku": " kulaklik-siyah ",
                      "attributes": {"renk": "siyah"},
                      "price": 2750.00,
                      "stockQuantity": 10
                    }
                  ]
                }
                """.formatted(categoryId);

        String response = mockMvc.perform(post("/api/v1/products")
                        .header("X-Seller-Id", 42)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/products/\\d+")))
                .andExpect(jsonPath("$.sellerId").value(42))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.categories[0].name").value("Elektronik"))
                .andExpect(jsonPath("$.variants[0].sku").value("KULAKLIK-SIYAH"))
                .andReturn().getResponse().getContentAsString();

        long productId = objectMapper.readTree(response).get("id").asLong();
        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.variants[0].attributes.renk").value("siyah"));
    }

    @Test
    void rejectsProductWithoutVariants() throws Exception {
        long categoryId = createCategory("Kitap");
        String productJson = """
                {
                  "name": "Java Kitabı",
                  "basePrice": 500.00,
                  "categoryIds": [%d],
                  "variants": []
                }
                """.formatted(categoryId);

        mockMvc.perform(post("/api/v1/products")
                        .header("X-Seller-Id", 42)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Doğrulama hatası"))
                .andExpect(jsonPath("$.errors.variants").exists());
    }

    @Test
    void rejectsDuplicateSkuWithinRequest() throws Exception {
        long categoryId = createCategory("Bilgisayar");
        String productJson = """
                {
                  "name": "Mekanik Klavye",
                  "basePrice": 1500.00,
                  "categoryIds": [%d],
                  "variants": [
                    {"sku": "KLAVYE-TR", "attributes": {}, "price": 1500.00, "stockQuantity": 5},
                    {"sku": " klavye-tr ", "attributes": {}, "price": 1600.00, "stockQuantity": 3}
                  ]
                }
                """.formatted(categoryId);

        mockMvc.perform(post("/api/v1/products")
                        .header("X-Seller-Id", 42)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Katalog çakışması"));
    }

    @Test
    void updatesDescriptionPriceAndVariantStockForOwningSeller() throws Exception {
        long categoryId = createCategory("Güncellenecek Ürünler");
        String created = createProduct(categoryId, 42, "Güncel Ürün", "GUNCEL-1");
        JsonNode createdJson = objectMapper.readTree(created);
        long productId = createdJson.get("id").asLong();
        long variantId = createdJson.get("variants").get(0).get("id").asLong();

        String updateJson = """
                {
                  "description": "Yenilenmiş açıklama",
                  "basePrice": 825.00,
                  "variants": [
                    {"id": %d, "price": 849.90, "stockQuantity": 3}
                  ]
                }
                """.formatted(variantId);

        mockMvc.perform(patch("/api/v1/products/{productId}", productId)
                        .header("X-Seller-Id", 42)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Yenilenmiş açıklama"))
                .andExpect(jsonPath("$.basePrice").value(825.00))
                .andExpect(jsonPath("$.variants[0].price").value(849.90))
                .andExpect(jsonPath("$.variants[0].stockQuantity").value(3));

        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Yenilenmiş açıklama"))
                .andExpect(jsonPath("$.variants[0].stockQuantity").value(3));
    }

    @Test
    void hidesProductFromAnotherSellerDuringUpdate() throws Exception {
        long categoryId = createCategory("Satıcı İzolasyonu");
        JsonNode product = objectMapper.readTree(createProduct(categoryId, 42, "Özel Ürün", "OZEL-1"));

        mockMvc.perform(patch("/api/v1/products/{productId}", product.get("id").asLong())
                        .header("X-Seller-Id", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Yetkisiz değişiklik\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Kaynak bulunamadı"));
    }

    private long createCategory(String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }

    private String createProduct(long categoryId, long sellerId, String name, String sku) throws Exception {
        String productJson = """
                {
                  "name": "%s",
                  "description": "İlk açıklama",
                  "basePrice": 750.00,
                  "categoryIds": [%d],
                  "variants": [
                    {"sku": "%s", "attributes": {}, "price": 775.00, "stockQuantity": 9}
                  ]
                }
                """.formatted(name, categoryId, sku);
        return mockMvc.perform(post("/api/v1/products")
                        .header("X-Seller-Id", sellerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
