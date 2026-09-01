package com.nexamarket.nexamarket.cart.infrastructure;

import com.nexamarket.common.integration.CatalogVariantSnapshot;
import com.nexamarket.common.integration.InternalRestClientFactory;
import com.nexamarket.nexamarket.cart.application.CatalogProductGateway;
import org.springframework.stereotype.Component;

@Component
public class RestCatalogProductGateway implements CatalogProductGateway {

    private final InternalRestClientFactory clients;

    public RestCatalogProductGateway(InternalRestClientFactory clients) {
        this.clients = clients;
    }

    @Override
    public CatalogVariantSnapshot findVariant(Long variantId) {
        return clients.create("catalog-service.base-url")
                .get()
                .uri("/internal/catalog/variants/{variantId}", variantId)
                .retrieve()
                .body(CatalogVariantSnapshot.class);
    }
}
