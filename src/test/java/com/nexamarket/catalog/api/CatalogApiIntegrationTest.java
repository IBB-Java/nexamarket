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

    private long createCategory(String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }
}
