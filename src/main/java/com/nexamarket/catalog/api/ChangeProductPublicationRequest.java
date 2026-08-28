package com.nexamarket.catalog.api;

import com.nexamarket.catalog.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

/**
 * The storefront only lists products that are actively published. A seller can
 * move a product between the visible and hidden states without changing its
 * catalogue details.
 */
public record ChangeProductPublicationRequest(@NotNull ProductStatus status) {

    public boolean isSupportedStorefrontStatus() {
        return status == ProductStatus.ACTIVE || status == ProductStatus.PASSIVE;
    }
}
