package com.nexamarket.catalog.repository;

import com.nexamarket.catalog.entity.Category;
import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.entity.ProductStatus;
import com.nexamarket.catalog.entity.ProductVariant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CatalogRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void persistsProductWithMultipleCategoriesAndVariantAttributes() {
        Category electronics = categoryRepository.save(Category.builder().name("Elektronik").build());
        Category headphones = categoryRepository.save(Category.builder()
                .name("Kulaklık")
                .parent(electronics)
                .build());

        Product product = Product.builder()
                .name("Kablosuz Kulaklık")
                .description("Aktif gürültü engelleme")
                .basePrice(new BigDecimal("2500.00"))
                .sellerId(42L)
                .status(ProductStatus.ACTIVE)
                .categories(Set.of(electronics, headphones))
                .build();
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku("KULAKLIK-SIYAH")
                .attributes(Map.of("renk", "siyah"))
                .price(new BigDecimal("2750.00"))
                .stockQuantity(10)
                .build();
        product.getVariants().add(variant);

        Product saved = productRepository.saveAndFlush(product);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCategories()).hasSize(2);
        assertThat(saved.getVariants()).singleElement()
                .extracting(ProductVariant::getSku)
                .isEqualTo("KULAKLIK-SIYAH");
    }
}
