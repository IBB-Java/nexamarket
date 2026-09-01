package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.common.integration.CatalogVariantSnapshot;

public interface CatalogProductGateway {

    CatalogVariantSnapshot findVariant(Long variantId);
}
