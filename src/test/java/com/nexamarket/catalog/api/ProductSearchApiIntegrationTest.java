package com.nexamarket.catalog.api;

import com.nexamarket.catalog.entity.ProductStatus;
import com.nexamarket.catalog.search.ProductSearchDocument;
import com.nexamarket.catalog.search.ProductSearchGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @BeforeEach
    void indexFixtures() {
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
