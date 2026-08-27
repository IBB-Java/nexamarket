package com.nexamarket.catalog.api;

import com.nexamarket.catalog.application.CatalogService;
import com.nexamarket.catalog.application.ProductSearchService;
import com.nexamarket.catalog.search.ProductSearchCriteria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final CatalogService catalogService;
    private final ProductSearchService productSearchService;

    @GetMapping("/search")
    public ProductSearchResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxPrice,
            @RequestParam(required = false) @DecimalMin("0.00") @DecimalMax("5.00") BigDecimal minSellerRating,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return productSearchService.search(new ProductSearchCriteria(
                q, categoryId, minPrice, maxPrice, minSellerRating, page, size));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @RequestHeader("X-Seller-Id") @Positive Long sellerId,
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = catalogService.createProduct(sellerId, request);
        return ResponseEntity.created(URI.create("/api/v1/products/" + response.id())).body(response);
    }

    @GetMapping("/{productId}")
    public ProductResponse get(@PathVariable @Positive Long productId) {
        return catalogService.getProduct(productId);
    }

    @PatchMapping("/{productId}")
    public ProductResponse update(
            @PathVariable @Positive Long productId,
            @RequestHeader("X-Seller-Id") @Positive Long sellerId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return catalogService.updateProduct(productId, sellerId, request);
    }
}
