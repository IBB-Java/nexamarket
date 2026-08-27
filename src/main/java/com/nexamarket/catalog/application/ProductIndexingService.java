package com.nexamarket.catalog.application;

import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.repository.ProductRepository;
import com.nexamarket.catalog.search.ProductSearchDocument;
import com.nexamarket.catalog.search.ProductSearchGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductIndexingService {

    private final ProductRepository productRepository;
    private final ProductSearchGateway productSearchGateway;

    @Transactional(readOnly = true)
    public void index(Long productId) {
        Product product = productRepository.findDetailedById(productId)
                .orElseThrow(() -> new CatalogNotFoundException("Ürün bulunamadı: " + productId));
        productSearchGateway.index(ProductSearchDocument.from(product, null));
    }
}
