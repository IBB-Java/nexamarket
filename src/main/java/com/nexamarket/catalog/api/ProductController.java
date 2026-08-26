package com.nexamarket.catalog.api;

import com.nexamarket.catalog.application.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final CatalogService catalogService;

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
}
