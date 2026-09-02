package com.nexamarket.catalog.application;

import com.nexamarket.catalog.api.ProductSearchResponse;
import com.nexamarket.catalog.api.ProductImageResponse;
import com.nexamarket.catalog.repository.ProductImageRepository;
import com.nexamarket.catalog.search.ProductSearchCriteria;
import com.nexamarket.catalog.search.ProductSearchGateway;
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
    private final SellerDirectoryGateway sellerDirectoryGateway;
    private final ProductImageRepository productImageRepository;

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
        Set<Long> productIds = result.items().stream()
                .map(document -> Long.valueOf(document.getId()))
                .collect(Collectors.toSet());
        Map<Long, String> sellerNames = sellerDirectoryGateway.displayNames(sellerIds);
        Map<Long, String> imageUrls = productImageRepository.findAllByProduct_IdInOrderByProduct_IdAscIdAsc(productIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getProduct().getId(),
                        image -> ProductImageResponse.from(image).originalUrl(),
                        (first, ignored) -> first
                ));
        return ProductSearchResponse.from(result, sellerNames, imageUrls);
    }
}
