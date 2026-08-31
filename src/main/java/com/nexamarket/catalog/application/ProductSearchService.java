package com.nexamarket.catalog.application;

import com.nexamarket.catalog.api.ProductSearchResponse;
import com.nexamarket.catalog.search.ProductSearchCriteria;
import com.nexamarket.catalog.search.ProductSearchGateway;
import com.nexamarket.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductSearchGateway productSearchGateway;
    private final UserAccountRepository userAccountRepository;

    @Cacheable(cacheNames = "catalogSearch", key = "#criteria.toString()")
    public ProductSearchResponse search(ProductSearchCriteria criteria) {
        if (criteria.minPrice() != null && criteria.maxPrice() != null
                && criteria.minPrice().compareTo(criteria.maxPrice()) > 0) {
            throw new InvalidSearchCriteriaException("Minimum fiyat, maksimum fiyattan büyük olamaz.");
        }
        var result = productSearchGateway.search(criteria);
        Set<Long> sellerIds = result.items().stream()
                .map(document -> document.getSellerId())
                .collect(Collectors.toSet());
        Map<Long, String> sellerNames = userAccountRepository.findAllById(sellerIds).stream()
                .collect(Collectors.toMap(user -> user.getId(), user -> user.getEmail()));
        return ProductSearchResponse.from(result, sellerNames);
    }
}
