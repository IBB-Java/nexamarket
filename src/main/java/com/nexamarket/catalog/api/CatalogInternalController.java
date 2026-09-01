package com.nexamarket.catalog.api;

import com.nexamarket.catalog.repository.ProductVariantRepository;
import com.nexamarket.common.integration.CatalogVariantSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/internal/catalog")
public class CatalogInternalController {

    private final ProductVariantRepository productVariantRepository;

    public CatalogInternalController(ProductVariantRepository productVariantRepository) {
        this.productVariantRepository = productVariantRepository;
    }

    @GetMapping("/variants/{variantId}")
    @Transactional(readOnly = true)
    public CatalogVariantSnapshot findVariant(@PathVariable Long variantId) {
        return productVariantRepository.findById(variantId)
                .map(variant -> new CatalogVariantSnapshot(
                        variant.getId(), variant.getProduct().getId(), variant.getProduct().getSellerId(),
                        variant.getProduct().getName(), variant.getPrice()))
                .orElseThrow(() -> new IllegalArgumentException("Product variant was not found."));
    }
}
