package com.nexamarket.catalog.application;

import com.nexamarket.catalog.api.ProductSearchResponse;
import com.nexamarket.catalog.search.ProductSearchCriteria;
import com.nexamarket.catalog.search.ProductSearchGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductSearchGateway productSearchGateway;

    @Cacheable(cacheNames = "catalogSearch", key = "#criteria.toString()")
    public ProductSearchResponse search(ProductSearchCriteria criteria) {
        if (criteria.minPrice() != null && criteria.maxPrice() != null
                && criteria.minPrice().compareTo(criteria.maxPrice()) > 0) {
            throw new InvalidSearchCriteriaException("Minimum fiyat, maksimum fiyattan büyük olamaz.");
        }
        return ProductSearchResponse.from(productSearchGateway.search(criteria));
    }
}
