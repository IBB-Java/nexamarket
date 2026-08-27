package com.nexamarket.catalog.api;

import com.nexamarket.catalog.entity.ProductImage;
import com.nexamarket.catalog.entity.ProductImageStatus;

public record ProductImageResponse(
        Long id,
        Long productId,
        ProductImageStatus status,
        String originalUrl,
        String thumbnailUrl
) {
    public static ProductImageResponse from(ProductImage image) {
        String baseUrl = "/api/v1/products/" + image.getProduct().getId() + "/images/" + image.getId();
        String thumbnailUrl = image.getStatus() == ProductImageStatus.READY ? baseUrl + "/thumbnail" : null;
        return new ProductImageResponse(
                image.getId(),
                image.getProduct().getId(),
                image.getStatus(),
                baseUrl + "/original",
                thumbnailUrl
        );
    }
}
