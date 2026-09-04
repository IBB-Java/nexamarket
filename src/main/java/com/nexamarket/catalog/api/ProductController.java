package com.nexamarket.catalog.api;

import com.nexamarket.catalog.application.CatalogService;
import com.nexamarket.catalog.application.ProductSearchService;
import com.nexamarket.auth.security.AuthPrincipal;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

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
    @PreAuthorize("isAnonymous() or hasAnyRole('CUSTOMER', 'SELLER', 'ADMIN')")
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
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = catalogService.createProduct(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/v1/products/" + response.id())).body(response);
    }

    @GetMapping("/{productId}")
    @PreAuthorize("isAnonymous() or hasAnyRole('CUSTOMER', 'SELLER', 'ADMIN')")
    public ProductResponse get(@PathVariable @Positive Long productId) {
        return catalogService.getProduct(productId);
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public java.util.List<ProductResponse> listSellerProducts(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return catalogService.listSellerProducts(principal.userId());
    }

    @PatchMapping("/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    public ProductResponse update(
            @PathVariable @Positive Long productId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return catalogService.updateProduct(productId, principal.userId(), request);
    }

    @PatchMapping("/{productId}/publication")
    @PreAuthorize("hasRole('SELLER')")
    public ProductResponse changePublication(
            @PathVariable @Positive Long productId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ChangeProductPublicationRequest request
    ) {
        return catalogService.changePublication(productId, principal.userId(), request);
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long productId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        catalogService.deleteProduct(productId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
